package com.example.ear.service;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
public class NaverOrcApiService {

    @Value("${naver.service.url}")
    private String url;

    /**
     * 네이버 ocr api 호출한다
     * @param {string} type 호출 메서드 타입
     * @param {string} filePath 파일 경로
     * @param {string} naver_secretKey 네이버 시크릿키 값
     * @param {string} ext 확장자
     * @returns {List} 추출 text list
     */
    public String callApi(String type, String filePath, String naver_secretKey, String ext) {
        String apiURL = url;
        String secretKey = naver_secretKey;
        String parseData = null;


        try {
            // API URL에 연결
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setReadTimeout(30000);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            // JSON 요청 메시지 생성
            JSONObject json = new JSONObject();
            json.put("version", "V2");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", System.currentTimeMillis());
            json.put("lang", "ko"); // 언어 설정

            // 이미지 객체 생성
            JSONObject image = new JSONObject();
            image.put("format", "jpg");
            image.put("name", "demo");
            image.put("url", filePath); // 이미지 URL 설정

            // images 배열에 이미지 객체 추가
            JSONArray images = new JSONArray();
            images.add(image);
            json.put("images", images);


            String postParams = json.toString(); // JSON 요청 문자열


            // API 요청 전송
            con.connect();

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(postParams.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            // 응답 코드 확인
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            List<String> parseDataList = jsonparse(response);
            StringBuilder sb = new StringBuilder();
            for (String s : parseDataList) {
                sb.append(s);
                sb.append(" ");
            }
            parseData = sb.toString();

        } catch (Exception e) {
            System.out.println(e);
        }
        return parseData;
    }

    /**
     * writeMultiPart
     * @param {OutputStream} out 데이터를 출력
     * @param {string} jsonMessage 요청 params
     * @param {File} file 요청 파일
     * @param {String} boundary 경계
     */
    private static void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
            IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
        sb.append(jsonMessage);
        sb.append("\r\n");

        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();

        if (file != null && file.isFile()) {
            out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            StringBuilder fileString = new StringBuilder();
            fileString
                    .append("Content-Disposition:form-data; name=\"file\"; filename=");
            fileString.append("\"" + file.getName() + "\"\r\n");
            fileString.append("Content-Type: application/octet-stream\r\n\r\n");
            out.write(fileString.toString().getBytes("UTF-8"));
            out.flush();

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.write("\r\n".getBytes());
            }

            out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        }
        out.flush();
    }
    /**
     * 데이터 가공
     * @param {StringBuffer} response 응답값
     * @returns {List} result text list
     */
    private static List<String> jsonparse(StringBuffer response) throws ParseException {
        // JSON 파싱
        JSONParser jp = new JSONParser();
        JSONObject jobj = (JSONObject) jp.parse(response.toString());

        // 'images' 배열을 JSONObject로 변환
        JSONArray jsonArrayPerson = (JSONArray) jobj.get("images");
        JSONObject jsonObjImage = (JSONObject) jsonArrayPerson.get(0);
        JSONArray fieldsArray = (JSONArray) jsonObjImage.get("fields");

        // 결과를 저장할 리스트
        List<String> result = new ArrayList<>();

        // 'fields' 배열에서 'inferText' 값을 추출
        for (Object fieldObj : fieldsArray) {
            JSONObject field = (JSONObject) fieldObj;
            String inferText = (String) field.get("inferText");
            if (inferText != null) {
                result.add(inferText);
            }
        }
        return result;
    }
}
