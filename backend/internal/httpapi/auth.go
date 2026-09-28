package httpapi

import (
	"encoding/json"
	"net/http"
	"strings"
)

type loginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type localUserInfo struct {
	Avatar   string   `json:"avatar"`
	RealName string   `json:"realName"`
	Roles    []string `json:"roles"`
	UserID   string   `json:"userId"`
	Username string   `json:"username"`
	Desc     string   `json:"desc"`
	HomePath string   `json:"homePath"`
	Token    string   `json:"token"`
}

func (s *Server) login(w http.ResponseWriter, r *http.Request) {
	var request loginRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil || strings.TrimSpace(request.Username) == "" || request.Password == "" {
		writeError(w, http.StatusBadRequest, "用户名和密码不能为空")
		return
	}

	writeJSON(w, http.StatusOK, map[string]string{"accessToken": localToken(request.Username)})
}

func (s *Server) refreshToken(w http.ResponseWriter, r *http.Request) {
	username := usernameFromToken(r.Header.Get("Authorization"))
	if username == "" {
		writeError(w, http.StatusUnauthorized, "登录状态已失效")
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"data": localToken(username), "status": 200})
}

func (s *Server) logout(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) accessCodes(w http.ResponseWriter, r *http.Request) {
	if usernameFromToken(r.Header.Get("Authorization")) == "" {
		writeError(w, http.StatusUnauthorized, "未登录")
		return
	}
	writeJSON(w, http.StatusOK, []string{"*"})
}

func (s *Server) userInfo(w http.ResponseWriter, r *http.Request) {
	username := usernameFromToken(r.Header.Get("Authorization"))
	if username == "" {
		writeError(w, http.StatusUnauthorized, "未登录")
		return
	}
	writeJSON(w, http.StatusOK, localUserInfo{
		Avatar:   "",
		RealName: username,
		Roles:    []string{"admin"},
		UserID:   "local-" + username,
		Username: username,
		Desc:     "本地控制面用户",
		HomePath: "/openresty/centers",
		Token:    localToken(username),
	})
}

func localToken(username string) string {
	return "local-" + strings.TrimSpace(username)
}

func usernameFromToken(header string) string {
	const prefix = "Bearer local-"
	if !strings.HasPrefix(header, prefix) {
		return ""
	}
	return strings.TrimSpace(strings.TrimPrefix(header, prefix))
}
