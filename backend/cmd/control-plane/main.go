package main

import (
	"log"
	"net/http"

	"net.daoke/openresty-plus-control-plane/internal/config"
	"net.daoke/openresty-plus-control-plane/internal/httpapi"
	"net.daoke/openresty-plus-control-plane/internal/store"
)

func main() {
	configuration, err := config.Load()
	if err != nil {
		log.Fatal(err)
	}
	database, err := store.Open(configuration.MySQLDSN)
	if err != nil {
		log.Fatal(err)
	}
	defer database.Close()
	if err := store.Migrate(database); err != nil {
		log.Fatal(err)
	}
	server := &http.Server{Addr: configuration.HTTPAddress, Handler: httpapi.New(database)}
	log.Printf("openresty-plus Go control plane listening on %s", configuration.HTTPAddress)
	log.Fatal(server.ListenAndServe())
}
