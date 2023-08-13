/*
 * Copyright (c) 2023-present ArmorAuth. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.armorauth.samples.controller;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwkSetController {
	private final JWKSource<SecurityContext> jwkSource;
	private final JWKSelector jwkSelector;

	public JwkSetController(JWKSource<SecurityContext> jwkSource) {
		Assert.notNull(jwkSource, "jwkSource cannot be null");
		this.jwkSource = jwkSource;
		this.jwkSelector = new JWKSelector(new JWKMatcher.Builder().build());
	}

	@GetMapping("/jwks")
	public Map<String, Object> getJwkSet() {
		JWKSet jwkSet;
		try {
			jwkSet = new JWKSet(this.jwkSource.get(this.jwkSelector, null));
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to select the JWK(s) -> " + ex.getMessage(), ex);
		}
		return jwkSet.toJSONObject();
	}

}