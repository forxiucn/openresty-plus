package httpapi

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
)

type httpServerView struct {
	ID                 string            `json:"id"`
	Domain             string            `json:"domain"`
	ListenPort         int               `json:"listenPort"`
	SSLEnabled         bool              `json:"sslEnabled"`
	CertificateID      *string           `json:"certificateId"`
	UpstreamID         *string           `json:"upstreamId"`
	AccessLog          string            `json:"accessLog"`
	ErrorLog           string            `json:"errorLog"`
	IPPolicyEnabled    bool              `json:"ipPolicyEnabled"`
	APIPolicyEnabled   bool              `json:"apiPolicyEnabled"`
	IPPolicyModeOrder  string            `json:"ipPolicyModeOrder"`
	APIPolicyModeOrder string            `json:"apiPolicyModeOrder"`
	RootPath           *string           `json:"rootPath"`
	HideVersion        bool              `json:"hideVersion"`
	ResponseHeaders    []string          `json:"responseHeaders"`
	ErrorPages         map[string]string `json:"errorPages"`
}
type httpServerRequest struct {
	Domain        string  `json:"domain"`
	ListenPort    int     `json:"listenPort"`
	SSLEnabled    bool    `json:"sslEnabled"`
	CertificateID *string `json:"certificateId"`
	UpstreamID    *string `json:"upstreamId"`
	AccessLog     string  `json:"accessLog"`
	ErrorLog      string  `json:"errorLog"`
}

func (server *Server) listHTTPServers(w http.ResponseWriter, r *http.Request) {
	center, ok := pathUUID(w, r, "centerID")
	if !ok {
		return
	}
	items, err := server.httpServers(center)
	if err != nil {
		writeError(w, 500, err.Error())
		return
	}
	writeJSON(w, 200, items)
}
func (server *Server) pageHTTPServers(w http.ResponseWriter, r *http.Request) {
	center, ok := pathUUID(w, r, "centerID")
	if !ok {
		return
	}
	items, err := server.httpServers(center)
	if err != nil {
		writeError(w, 500, err.Error())
		return
	}
	writeJSON(w, 200, paginate(r, items))
}
func (server *Server) httpServers(center uuid.UUID) ([]httpServerView, error) {
	rows, err := server.db.Query(`SELECT id,domain,listen_port,ssl_enabled,certificate_id,upstream_id,access_log,error_log,ip_policy_enabled,api_policy_enabled,ip_policy_mode_order,api_policy_mode_order,root_path,hide_version,response_headers,error_pages FROM http_server WHERE center_id=? ORDER BY domain,listen_port`, center[:])
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := []httpServerView{}
	for rows.Next() {
		var raw, certificate, upstream []byte
		var headers, errors []byte
		var item httpServerView
		if err := rows.Scan(&raw, &item.Domain, &item.ListenPort, &item.SSLEnabled, &certificate, &upstream, &item.AccessLog, &item.ErrorLog, &item.IPPolicyEnabled, &item.APIPolicyEnabled, &item.IPPolicyModeOrder, &item.APIPolicyModeOrder, &item.RootPath, &item.HideVersion, &headers, &errors); err != nil {
			return nil, err
		}
		item.ID = uuidText(raw)
		if len(certificate) > 0 {
			value := uuidText(certificate)
			item.CertificateID = &value
		}
		if len(upstream) > 0 {
			value := uuidText(upstream)
			item.UpstreamID = &value
		}
		_ = json.Unmarshal(headers, &item.ResponseHeaders)
		_ = json.Unmarshal(errors, &item.ErrorPages)
		if item.ResponseHeaders == nil {
			item.ResponseHeaders = []string{}
		}
		if item.ErrorPages == nil {
			item.ErrorPages = map[string]string{}
		}
		items = append(items, item)
	}
	return items, rows.Err()
}
func (server *Server) createHTTPServer(w http.ResponseWriter, r *http.Request) {
	center, ok := pathUUID(w, r, "centerID")
	if !ok {
		return
	}
	var input httpServerRequest
	if !validHTTPServer(w, r, &input) {
		return
	}
	certificate, ok := optionalUUID(w, input.CertificateID)
	if !ok {
		return
	}
	upstream, ok := optionalUUID(w, input.UpstreamID)
	if !ok {
		return
	}
	if input.SSLEnabled && certificate == nil {
		writeError(w, 400, "TLS server must select a certificate")
		return
	}
	id := uuid.New()
	access, errorLog := serverLogPaths(input)
	_, err := server.db.Exec(`INSERT INTO http_server (id,center_id,domain,listen_port,ssl_enabled,certificate_id,upstream_id,access_log,error_log,ip_policy_enabled,api_policy_enabled,ip_policy_mode_order,api_policy_mode_order,root_path,hide_version,response_headers,error_pages,created_at) VALUES (?,?,?,?,?,?,?,?,?,false,false,'BLACKLIST_FIRST','BLACKLIST_FIRST',NULL,true,JSON_ARRAY(),JSON_OBJECT(),?)`, id[:], center[:], strings.TrimSpace(input.Domain), input.ListenPort, input.SSLEnabled, uuidBytes(certificate), uuidBytes(upstream), access, errorLog, time.Now())
	if err != nil {
		writeError(w, 409, err.Error())
		return
	}
	server.audit(center, "HTTP_SERVER_CREATED", "HTTP_SERVER", id)
	item, _ := server.findHTTPServer(center, id)
	writeJSON(w, 201, item)
}
func (server *Server) updateHTTPServer(w http.ResponseWriter, r *http.Request) {
	center, ok := pathUUID(w, r, "centerID")
	if !ok {
		return
	}
	id, ok := pathUUID(w, r, "serverID")
	if !ok {
		return
	}
	var input httpServerRequest
	if !validHTTPServer(w, r, &input) {
		return
	}
	certificate, ok := optionalUUID(w, input.CertificateID)
	if !ok {
		return
	}
	upstream, ok := optionalUUID(w, input.UpstreamID)
	if !ok {
		return
	}
	if input.SSLEnabled && certificate == nil {
		writeError(w, 400, "TLS server must select a certificate")
		return
	}
	access, errorLog := serverLogPaths(input)
	result, err := server.db.Exec("UPDATE http_server SET domain=?,listen_port=?,ssl_enabled=?,certificate_id=?,upstream_id=?,access_log=?,error_log=? WHERE id=? AND center_id=?", strings.TrimSpace(input.Domain), input.ListenPort, input.SSLEnabled, uuidBytes(certificate), uuidBytes(upstream), access, errorLog, id[:], center[:])
	if err != nil {
		writeError(w, 500, err.Error())
		return
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		writeError(w, 404, "HTTP server not found")
		return
	}
	server.audit(center, "HTTP_SERVER_UPDATED", "HTTP_SERVER", id)
	item, _ := server.findHTTPServer(center, id)
	writeJSON(w, 200, item)
}
func (server *Server) deleteHTTPServer(w http.ResponseWriter, r *http.Request) {
	center, ok := pathUUID(w, r, "centerID")
	if !ok {
		return
	}
	id, ok := pathUUID(w, r, "serverID")
	if !ok {
		return
	}
	result, err := server.db.Exec("DELETE FROM http_server WHERE id=? AND center_id=?", id[:], center[:])
	if err != nil {
		writeError(w, 500, err.Error())
		return
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		writeError(w, 404, "HTTP server not found")
		return
	}
	server.audit(center, "HTTP_SERVER_DELETED", "HTTP_SERVER", id)
	w.WriteHeader(204)
}
func (server *Server) findHTTPServer(center, id uuid.UUID) (httpServerView, error) {
	items, err := server.httpServers(center)
	if err != nil {
		return httpServerView{}, err
	}
	for _, item := range items {
		if item.ID == id.String() {
			return item, nil
		}
	}
	return httpServerView{}, nil
}
func validHTTPServer(w http.ResponseWriter, r *http.Request, input *httpServerRequest) bool {
	if !decode(r, input) || strings.TrimSpace(input.Domain) == "" || input.ListenPort < 1 || input.ListenPort > 65535 {
		writeError(w, 400, "domain and listenPort are required")
		return false
	}
	return true
}
func optionalUUID(w http.ResponseWriter, value *string) (*uuid.UUID, bool) {
	if value == nil || strings.TrimSpace(*value) == "" {
		return nil, true
	}
	id, err := uuid.Parse(*value)
	if err != nil {
		writeError(w, 400, "invalid identifier")
		return nil, false
	}
	return &id, true
}
func uuidBytes(value *uuid.UUID) any {
	if value == nil {
		return nil
	}
	return (*value)[:]
}
func serverLogPaths(input httpServerRequest) (string, string) {
	domain := strings.TrimSpace(input.Domain)
	access := strings.TrimSpace(input.AccessLog)
	if access == "" {
		access = "/var/log/nginx/" + domain + "." + stringPort(input.ListenPort) + ".access.log"
	}
	errorLog := strings.TrimSpace(input.ErrorLog)
	if errorLog == "" {
		errorLog = "/var/log/nginx/" + domain + "." + stringPort(input.ListenPort) + ".error.log"
	}
	return access, errorLog
}
func stringPort(port int) string { return strconv.Itoa(port) }
