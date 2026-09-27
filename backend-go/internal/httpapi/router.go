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
