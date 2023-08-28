package com.armorauth;

import lombok.Value;

import java.util.List;

@Value
public class HomepageForwardingFilterConfig {

    String homepage;

    List<String> routesIncludes;

}