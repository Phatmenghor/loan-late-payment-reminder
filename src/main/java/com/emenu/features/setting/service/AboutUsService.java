package com.emenu.features.setting.service;

import com.emenu.features.setting.dto.request.AboutUsCreateRequest;
import com.emenu.features.setting.dto.response.AboutUsResponse;
import com.emenu.features.setting.dto.update.AboutUsUpdateRequest;

public interface AboutUsService {

    AboutUsResponse getAboutUs();
    AboutUsResponse createOrUpdate(AboutUsCreateRequest request);
    AboutUsResponse update(AboutUsUpdateRequest request);
}
