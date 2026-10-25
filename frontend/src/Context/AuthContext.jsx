import { createContext, useContext, useState } from "react";
import API from "../axios";

const AuthContext = createContext(null);

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem("user"));
  } catch {
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(readStoredUser);

  const startSession = ({ token, id, name, email, role }) => {
    const loggedIn = { id, name, email, role };
    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(loggedIn));
    setUser(loggedIn);
    return loggedIn;
  };

  const login = async (email, password) => {
    const { data } = await API.post("/auth/login", { email, password });
    return startSession(data);
  };

  const register = async (name, email, password) => {
    const { data } = await API.post("/auth/register", { name, email, password });
    return startSession(data);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{ user, isAdmin: user?.role === "ADMIN", login, register, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
