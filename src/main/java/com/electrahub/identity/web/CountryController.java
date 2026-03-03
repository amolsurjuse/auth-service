package com.electrahub.identity.web;

import com.electrahub.identity.integration.UserServiceClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final UserServiceClient userServiceClient;

    public CountryController(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public record CountryView(String code, String name, String dialCode) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountryView> list() {
        return userServiceClient.listCountries().stream()
                .map(c -> new CountryView(c.code(), c.name(), c.dialCode()))
                .toList();
    }
}
