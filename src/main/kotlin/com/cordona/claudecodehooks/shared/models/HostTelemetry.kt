package com.cordona.claudecodehooks.shared.models

import com.fasterxml.jackson.annotation.JsonProperty

data class HostTelemetry(
	@field:JsonProperty("daemon_details")
	val daemon: DaemonDetails?,
	@field:JsonProperty("host_details")
	val hostDetails: HostDetails,
	@field:JsonProperty("tmux_session")
	val tmuxSession: TmuxSession?,
) {
	data class DaemonDetails(
		val id: String,
		val pid: Int,
	)

	data class HostDetails(
		val hostname: String,
		val platform: String,
		@field:JsonProperty("private_ip")
		val privateIp: String,
		@field:JsonProperty("public_ip")
		val publicIp: String,
		val username: String,
	)

	data class TmuxSession(
		@field:JsonProperty("session_id")
		val sessionId: String,
		@field:JsonProperty("session_name")
		val sessionName: String,
		@field:JsonProperty("pane_id")
		val paneId: String,
	)
}