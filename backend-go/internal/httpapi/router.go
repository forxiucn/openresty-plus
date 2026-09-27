package httpapi

import (
	"database/sql"
	"encoding/json"
	"net/http"
)

// New builds the control-plane HTTP surface. Store-backed API modules are
// registered here as they are migrated from the Java control plane.
type Server struct{ db *sql.DB }

func New(database *sql.DB) http.Handler {
	mux := http.NewServeMux()
	server := &Server{db: database}
	mux.HandleFunc("GET /healthz", healthz)
	mux.HandleFunc("GET /api/centers", server.listCenters)
	mux.HandleFunc("GET /api/centers/paged", server.pageCenters)
	mux.HandleFunc("POST /api/centers", server.createCenter)
	mux.HandleFunc("PUT /api/centers/{centerID}", server.updateCenter)
	mux.HandleFunc("DELETE /api/centers/{centerID}", server.deleteCenter)
	mux.HandleFunc("GET /api/centers/{centerID}/nodes", server.listNodes)
	mux.HandleFunc("GET /api/centers/{centerID}/nodes/paged", server.pageNodes)
	mux.HandleFunc("POST /api/centers/{centerID}/nodes", server.createNode)
	mux.HandleFunc("PUT /api/centers/{centerID}/nodes/{nodeID}", server.updateNode)
	mux.HandleFunc("DELETE /api/centers/{centerID}/nodes/{nodeID}", server.deleteNode)
	mux.HandleFunc("GET /api/centers/{centerID}/http/upstreams", server.listHTTPUpstreams)
	mux.HandleFunc("GET /api/centers/{centerID}/http/upstreams/paged", server.pageHTTPUpstreams)
	mux.HandleFunc("POST /api/centers/{centerID}/http/upstreams", server.createHTTPUpstream)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/upstreams/{upstreamID}", server.updateHTTPUpstream)
	mux.HandleFunc("DELETE /api/centers/{centerID}/http/upstreams/{upstreamID}", server.deleteHTTPUpstream)
	mux.HandleFunc("GET /api/centers/{centerID}/http/upstreams/{upstreamID}/targets/paged", server.pageHTTPUpstreamTargets)
	mux.HandleFunc("POST /api/centers/{centerID}/http/upstreams/{upstreamID}/targets", server.createHTTPUpstreamTarget)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/upstreams/{upstreamID}/targets/{targetID}", server.updateHTTPUpstreamTarget)
	mux.HandleFunc("DELETE /api/centers/{centerID}/http/upstreams/{upstreamID}/targets/{targetID}", server.deleteHTTPUpstreamTarget)
	mux.HandleFunc("GET /api/centers/{centerID}/http/settings", server.getHTTPSettings)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/settings", server.putHTTPSettings)
	mux.HandleFunc("GET /api/centers/{centerID}/http/servers", server.listHTTPServers)
	mux.HandleFunc("GET /api/centers/{centerID}/http/servers/paged", server.pageHTTPServers)
	mux.HandleFunc("POST /api/centers/{centerID}/http/servers", server.createHTTPServer)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}", server.updateHTTPServer)
	mux.HandleFunc("DELETE /api/centers/{centerID}/http/servers/{serverID}", server.deleteHTTPServer)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}/policy-settings", server.putHTTPServerPolicySettings)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}/directives", server.putHTTPServerDirectives)
	mux.HandleFunc("GET /api/centers/{centerID}/http/servers/locations/paged", server.pageCenterHTTPLocations)
	mux.HandleFunc("GET /api/centers/{centerID}/http/servers/{serverID}/locations", server.listHTTPLocations)
	mux.HandleFunc("GET /api/centers/{centerID}/http/servers/{serverID}/locations/paged", server.pageHTTPLocations)
	mux.HandleFunc("POST /api/centers/{centerID}/http/servers/{serverID}/locations", server.createHTTPLocation)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}/locations/{locationID}", server.updateHTTPLocation)
	mux.HandleFunc("DELETE /api/centers/{centerID}/http/servers/{serverID}/locations/{locationID}", server.deleteHTTPLocation)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}/locations/{locationID}/policy-settings", server.putHTTPLocationPolicySettings)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}/locations/{locationID}/directives", server.putHTTPLocationDirectives)
	mux.HandleFunc("PUT /api/centers/{centerID}/http/servers/{serverID}/locations/{locationID}/dynamic-dns", server.putHTTPLocationDynamicDNS)
	mux.HandleFunc("GET /api/centers/{centerID}/tls-certificates", server.listTLSCertificates)
	mux.HandleFunc("POST /api/centers/{centerID}/tls-certificates", server.createTLSCertificate)
	mux.HandleFunc("PUT /api/centers/{centerID}/tls-certificates/{certificateID}", server.updateTLSCertificate)
	mux.HandleFunc("DELETE /api/centers/{centerID}/tls-certificates/{certificateID}", server.deleteTLSCertificate)
	mux.HandleFunc("GET /api/centers/{centerID}/stream/upstreams", server.listStreamUpstreams)
	mux.HandleFunc("GET /api/centers/{centerID}/stream/upstreams/paged", server.pageStreamUpstreams)
	mux.HandleFunc("POST /api/centers/{centerID}/stream/upstreams", server.createStreamUpstream)
	mux.HandleFunc("PUT /api/centers/{centerID}/stream/upstreams/{upstreamID}", server.updateStreamUpstream)
	mux.HandleFunc("DELETE /api/centers/{centerID}/stream/upstreams/{upstreamID}", server.deleteStreamUpstream)
	mux.HandleFunc("GET /api/centers/{centerID}/stream/servers", server.listStreamServers)
	mux.HandleFunc("GET /api/centers/{centerID}/stream/servers/paged", server.pageStreamServers)
	mux.HandleFunc("POST /api/centers/{centerID}/stream/servers", server.createStreamServer)
	mux.HandleFunc("PUT /api/centers/{centerID}/stream/servers/{streamServerID}", server.updateStreamServer)
	mux.HandleFunc("DELETE /api/centers/{centerID}/stream/servers/{streamServerID}", server.deleteStreamServer)
	mux.HandleFunc("PUT /api/centers/{centerID}/stream/servers/{streamServerID}/dynamic-dns", server.putStreamServerDynamicDNS)
	mux.HandleFunc("PUT /api/centers/{centerID}/stream/servers/{streamServerID}/policy-settings", server.putStreamServerPolicySettings)
	mux.HandleFunc("GET /api/centers/{centerID}/dns-resolvers", server.listDNSResolvers)
	mux.HandleFunc("GET /api/centers/{centerID}/dns-resolvers/paged", server.pageDNSResolvers)
	mux.HandleFunc("POST /api/centers/{centerID}/dns-resolvers", server.createDNSResolver)
	mux.HandleFunc("PUT /api/centers/{centerID}/dns-resolvers/{resolverID}", server.updateDNSResolver)
	mux.HandleFunc("DELETE /api/centers/{centerID}/dns-resolvers/{resolverID}", server.deleteDNSResolver)
	mux.HandleFunc("GET /api/centers/{centerID}/audit-events", server.listAuditEvents)
	mux.HandleFunc("GET /api/centers/{centerID}/audit-events/paged", server.pageAuditEvents)
	return mux
}

func healthz(writer http.ResponseWriter, _ *http.Request) {
	writeJSON(writer, http.StatusOK, map[string]string{"status": "ok"})
}

func writeJSON(writer http.ResponseWriter, status int, value any) {
	writer.Header().Set("Content-Type", "application/json; charset=utf-8")
	writer.WriteHeader(status)
	_ = json.NewEncoder(writer).Encode(value)
}
func writeError(writer http.ResponseWriter, status int, detail string) {
	writeJSON(writer, status, map[string]string{"detail": detail})
}
