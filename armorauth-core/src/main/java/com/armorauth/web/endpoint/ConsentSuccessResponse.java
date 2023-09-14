package com.armorauth.web.endpoint;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConsentSuccessResponse implements Serializable {

    private String redirectUri;

}
