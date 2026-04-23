package com.backend.features.src.main.java.com.cpbank.helper;

import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonHelper {
    @Autowired
    private final GenerateSignKey generateSignKey;

    public String createJsonPayload(String phone, String content) throws Exception {
        JSONObject jsonObject = new JSONObject();
        String signKey = generateSignKey.getSignKey(phone, content);
        try {
            jsonObject.put("phone", phone);
            jsonObject.put("content", content);
            jsonObject.put("signKey", signKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
