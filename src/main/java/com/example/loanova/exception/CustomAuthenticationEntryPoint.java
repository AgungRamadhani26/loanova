package com.example.loanova.exception;

import com.example.loanova.base.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler untuk 401 Unauthorized
 * Dipanggil ketika user belum login atau token invalid
 * Contoh: Akses endpoint tanpa token, token expired
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

   @Override
   public void commence(
         HttpServletRequest request,
         HttpServletResponse response,
         AuthenticationException authException) throws IOException, ServletException {

      // Set response 401 Unauthorized
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      // Build response
      ApiResponse<Object> apiResponse = ApiResponse.builder()
            .success(false)
            .message("Autentikasi gagal. Silakan login terlebih dahulu")
            .data(null)
            .build();

      // Convert ke JSON dan kirim
      ObjectMapper mapper = new ObjectMapper();
      String jsonResponse = mapper.writeValueAsString(apiResponse);
      response.getWriter().write(jsonResponse);
   }
}
