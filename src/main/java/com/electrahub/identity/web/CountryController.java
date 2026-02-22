package com.electrahub.identity.web;

import com.electrahub.identity.repository.CountryRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    private final CountryRepository countryRepository;

    public CountryController(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public record CountryView(String code, String name, String dialCode) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountryView> list() {
        return countryRepository.findByEnabledTrueOrderByNameAsc().stream()
                .map(c -> new CountryView(c.getIsoCode(), c.getName(), c.getDialCode()))
                .toList();
    }
}
