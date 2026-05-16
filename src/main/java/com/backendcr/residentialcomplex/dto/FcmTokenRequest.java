package com.backendcr.residentialcomplex.dto;

import jakarta.validation.constraints.NotBlank;

public class FcmTokenRequest {

    @NotBlank(message = "El token FCM es requerido")
    private String token;

    @NotBlank(message = "La plataforma es requerida")
    private String plataforma; // ANDROID, IOS, WEB

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma.toUpperCase(); }
}
