package com.cordona.claudecodehooks.infrastructure.internal.config

import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties.SseProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class VirtualThreadSchedulingConfig(
	private val sseProperties: SseProperties,
) : SchedulingConfigurer {

	override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
		val scheduler = SimpleAsyncTaskScheduler().apply {
			setVirtualThreads(true)
			setThreadNamePrefix(VIRTUAL_THREAD_NAME_PREFIX)
			concurrencyLimit = sseProperties.concurrency.virtualThreadsLimit
		}
		taskRegistrar.setTaskScheduler(scheduler)
	}

	companion object {
		private const val VIRTUAL_THREAD_NAME_PREFIX = "virtual-thread-heartbeat-"
	}
}