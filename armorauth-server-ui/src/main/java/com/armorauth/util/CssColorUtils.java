package com.armorauth.util;

import java.util.regex.Pattern;

public class CssColorUtils {

	private static final Pattern HEX_RGB_PATTERN = Pattern.compile("^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$");

	public String hexToRgb(String color) {
		if (!HEX_RGB_PATTERN.matcher(color).matches()) {
			throw new IllegalArgumentException(String.format("Invalid hex rgb format %s", color));
		}
		return String.format("%s, %s, %s", Integer.valueOf(color.substring(1, 3), 16),
				Integer.valueOf(color.substring(3, 5), 16), Integer.valueOf(color.substring(5, 7), 16));
	}

}