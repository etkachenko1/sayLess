import { getIdFromToken } from "./getIdFromToken";
import { API_URL } from "../config/api";

const USERS_API = `${API_URL}/users`

export interface UserProfile {
    id: string;
    username: string;
    email?: string;
    profilePic?: string;
    bio?: string;
}

export async function fetchUserProfile(): Promise<UserProfile | null> {
    const userId = getIdFromToken();
    if (!userId) return null;

    const token = localStorage.getItem("token");

    try {
        const res = await fetch(`${USERS_API}/${userId}`, {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
        });
        if (res.status === 401 || res.status === 404) {
            localStorage.removeItem("token");
            return null;
        }
        if(!res.ok) throw new Error("Failed to fetch user info");
        return await res.json();
    } catch(err) {
        console.error("Error fetching username: ", err);
        return null;
    }
}