package com.albertogeniola.merossconf.model;


import androidx.annotation.NonNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MqttConfiguration {
    private String name;
    private String hostname;
    private int port;

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
