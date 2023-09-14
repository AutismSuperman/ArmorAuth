package com.armorauth.web.http.converter;

import com.armorauth.web.endpoint.ConsentSuccessResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConsentSuccessResponseHttpMessageConverter extends AbstractHttpMessageConverter<ConsentSuccessResponse> {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


    private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<>() {
    };



    private GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();


    private final ObjectMapper objectMapper = new ObjectMapper();

    private Converter<Map<String, Object>, ConsentSuccessResponse> consentSuccessResponseConverter = this::consentSuccessResponseMapConverter;


    private Converter<ConsentSuccessResponse, Map<String, Object>> consentSuccessResponseParametersConverter = this::consentSuccessResponseParametersConverter;


    public ConsentSuccessResponseHttpMessageConverter() {
        super(DEFAULT_CHARSET, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    }


    @Override
    protected boolean supports(Class<?> clazz) {
        return ConsentSuccessResponse.class.isAssignableFrom(clazz);
    }

    @Override
    protected ConsentSuccessResponse readInternal(Class<? extends ConsentSuccessResponse> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        try {
            Map<String, Object> loginSuccessParameters = (Map<String, Object>) this.jsonMessageConverter
                    .read(STRING_OBJECT_MAP.getType(), null, inputMessage);
            return consentSuccessResponseConverter.convert(loginSuccessParameters);
        } catch (Exception ex) {
            throw new HttpMessageNotReadableException(
                    "An error occurred reading the Login Success Response: " + ex.getMessage(), ex,
                    inputMessage);
        }
    }

    @Override
    protected void writeInternal(ConsentSuccessResponse consentSuccessResponse, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            Map<String, Object> loginSuccessResponseParameters = consentSuccessResponseParametersConverter.convert(consentSuccessResponse);
            Assert.notNull(loginSuccessResponseParameters, "loginSuccessResponseParameters cannot be null");
            this.jsonMessageConverter.write(loginSuccessResponseParameters, STRING_OBJECT_MAP.getType(),
                    MediaType.APPLICATION_JSON, outputMessage);
        } catch (Exception ex) {
            throw new HttpMessageNotWritableException("An error occurred writing the Consent Success Response: " + ex.getMessage(), ex);
        }
    }

    private ConsentSuccessResponse consentSuccessResponseMapConverter(Map<String, Object> source) {
        return objectMapper.convertValue(source, new TypeReference<>() {
        });
    }

    private Map<String, Object> consentSuccessResponseParametersConverter(ConsentSuccessResponse source) {
        return objectMapper.convertValue(source, new TypeReference<>() {
        });
    }


    public void setConsentSuccessResponseConverter(Converter<Map<String, Object>, ConsentSuccessResponse> consentSuccessResponseConverter) {
        Assert.notNull(consentSuccessResponseConverter, "consentSuccessResponseConverter cannot be null");
        this.consentSuccessResponseConverter = consentSuccessResponseConverter;
    }

    public void setConsentSuccessResponseParametersConverter(Converter<ConsentSuccessResponse, Map<String, Object>> consentSuccessResponseParametersConverter) {
        Assert.notNull(consentSuccessResponseParametersConverter, "consentSuccessResponseParametersConverter cannot be null");
        this.consentSuccessResponseParametersConverter = consentSuccessResponseParametersConverter;
    }


}
