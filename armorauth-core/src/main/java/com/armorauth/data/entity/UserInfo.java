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
package com.armorauth.data.entity;


import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * 用户信息表
 * @author fulin
 * @since 2022-08-31
 */
@Data
@Entity
@Table(name = "user_info")
public class UserInfo {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @Id
    @GenericGenerator(name = "uuid-hex", strategy = "uuid.hex")
    @GeneratedValue(generator = "uuid-hex")
    private String id;


    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password", nullable = false)
    private String password;


    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 用户账号状态 0：正常 1：冻结 2：禁用 3: 注销
     */
    @Column(name = "status", columnDefinition = "int default 0", nullable = false, length = 1)
    private Integer status;

    @Column(name = "create_time", columnDefinition = "datetime", nullable = false)
    private String createTime;

    @Column(name = "last_login_time", columnDefinition = "datetime", nullable = false)
    private String lastLoginTime;

}
