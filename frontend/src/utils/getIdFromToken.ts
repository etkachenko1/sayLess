import { jwtDecode } from "jwt-decode";

interface JwtPayload {
    sub?: string;
    exp?: number;
    iat?: number;
}

export function getIdFromToken(): string | null {
    const token = localStorage.getItem("token");
    if (!token) return null;

    try {
        const decoded = jwtDecode<JwtPayload>(token);
        return decoded.sub || null;
    } catch(err) {
        console.error("Failed to decode JWT", err);
        return null;
    }

}