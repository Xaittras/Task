package com.example.authapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ProcessRequest(@NotBlank String text) {
}
