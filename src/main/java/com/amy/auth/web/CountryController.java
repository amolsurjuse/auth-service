package com.amy.auth.web;

import com.amy.auth.domain.enums.CountryCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    public record CountryView(String code, String name) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountryView> list() {
        return Arrays.stream(CountryCode.values())
                .map(c -> new CountryView(c.name(), c.displayName()))
                .toList();
    }
}
