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
		writeAuthError(w, http.StatusBadRequest, "用户名和密码不能为空")
		return
	}

	writeAuthData(w, http.StatusOK, map[string]string{"accessToken": localToken(request.Username)})
}

func (s *Server) refreshToken(w http.ResponseWriter, r *http.Request) {
	username := usernameFromToken(r.Header.Get("Authorization"))
	if username == "" {
		writeAuthError(w, http.StatusUnauthorized, "登录状态已失效")
		return
	}
	// refreshTokenApi 使用未注册标准 code/data 拦截器的 baseRequestClient，
	// 因此保持其约定的响应结构。
	writeJSON(w, http.StatusOK, map[string]any{"data": localToken(username), "status": 200})
}

func (s *Server) logout(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) accessCodes(w http.ResponseWriter, r *http.Request) {
	if usernameFromToken(r.Header.Get("Authorization")) == "" {
		writeAuthError(w, http.StatusUnauthorized, "未登录")
		return
	}
	writeAuthData(w, http.StatusOK, []string{"*"})
}

func (s *Server) userInfo(w http.ResponseWriter, r *http.Request) {
	username := usernameFromToken(r.Header.Get("Authorization"))
	if username == "" {
		writeAuthError(w, http.StatusUnauthorized, "未登录")
		return
	}
	writeAuthData(w, http.StatusOK, localUserInfo{
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

func writeAuthData(w http.ResponseWriter, status int, data any) {
	writeJSON(w, status, map[string]any{"code": 0, "data": data, "message": "ok"})
}

func writeAuthError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]any{"code": status, "data": nil, "message": message})
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
