package net.daoke.openrestyplus.health;

/** Probe protocol used by the control plane. PING is a TCP connect probe. */
public enum HealthCheckType { TCP, HTTP, PING }
