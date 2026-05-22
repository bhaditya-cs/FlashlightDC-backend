package org.flashlightdc.flashlight.config;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class PageMixin<T> {

    @JsonCreator
    public PageMixin(@JsonProperty("content") List<T> content,
                     @JsonProperty("pageable") Pageable pageable,
                     @JsonProperty("total") long total) {
    }

}