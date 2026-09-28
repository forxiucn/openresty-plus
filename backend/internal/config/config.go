package config

import (
	"fmt"
	"net/url"
	"os"
	"strings"
)

type Config struct {
	HTTPAddress string
	MySQLDSN    string
}

func Load() (Config, error) {
	address := os.Getenv("OPENRESTY_HTTP_ADDR")
	if address == "" {
		address = ":8081"
	}
	jdbcURL := strings.TrimPrefix(os.Getenv("OPENRESTY_DB_URL"), "jdbc:")
	if jdbcURL == "" {
		return Config{}, fmt.Errorf("OPENRESTY_DB_URL is required")
	}
	parsed, err := url.Parse(jdbcURL)
	if err != nil {
		return Config{}, fmt.Errorf("parse OPENRESTY_DB_URL: %w", err)
	}
	user := url.QueryEscape(os.Getenv("OPENRESTY_DB_USERNAME"))
	password := url.QueryEscape(os.Getenv("OPENRESTY_DB_PASSWORD"))
	if user == "" {
		return Config{}, fmt.Errorf("OPENRESTY_DB_USERNAME is required")
	}
	return Config{HTTPAddress: address, MySQLDSN: fmt.Sprintf("%s:%s@tcp(%s)%s?parseTime=true&charset=utf8mb4&multiStatements=true", user, password, parsed.Host, parsed.EscapedPath())}, nil
}
