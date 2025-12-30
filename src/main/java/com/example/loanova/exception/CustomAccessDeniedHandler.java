package com.example.loanova.exception;

import com.example.loanova.base.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handler untuk 403 Forbidden
 * Dipanggil ketika user tidak punya akses ke endpoint
 * Contoh: MARKETING coba akses endpoint SUPERADMIN
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

   @Override
   public void handle(
         HttpServletRequest request,
         HttpServletResponse response,
         AccessDeniedException accessDeniedException) throws IOException, ServletException {

      // Set response 403 Forbidden
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      // Build response
      ApiResponse<Object> apiResponse = ApiResponse.builder()
            .success(false)
            .message("Anda tidak memiliki akses untuk mengakses resource ini")
            .data(null)
            .build();

      // Convert ke JSON dan kirim
      ObjectMapper mapper = new ObjectMapper();
      String jsonResponse = mapper.writeValueAsString(apiResponse);
      response.getWriter().write(jsonResponse);
   }
}
