package com.cordona.claudecodehooks.base

import org.springframework.test.context.TestPropertySource

@TestPropertySource(locations = ["classpath:test.env"])
abstract class BaseTest