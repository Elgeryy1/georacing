import React, { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { 
  User, 
  signOut,
  onAuthStateChanged
} from "firebase/auth";
import { auth } from "../firebase/firebaseApp";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setUser(user);
      setLoading(false);
    });

    return unsubscribe;
  }, []);

  const login = async (email: string) => {
    // Login simplificado sin contraseña
    const mockUser = { email } as User;
    setUser(mockUser);
  };

  const logout = async () => {
    await signOut(auth);
    // The mock login user never existed in Firebase, so onAuthStateChanged
    // won't fire to clear it — reset local state explicitly.
    setUser(null);
  };

  const value = {
    user,
    loading,
    login,
    logout
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
