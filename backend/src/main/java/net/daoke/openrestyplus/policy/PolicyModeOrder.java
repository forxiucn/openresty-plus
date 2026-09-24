package net.daoke.openrestyplus.policy;

/** Resolves a request that matches both a blacklist and a whitelist. */
public enum PolicyModeOrder {
    BLACKLIST_FIRST,
    WHITELIST_FIRST
}
