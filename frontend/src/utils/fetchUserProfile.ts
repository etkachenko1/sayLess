import { getIdFromToken } from "./getIdFromToken";

const AUTH_API = "http://localhost:8081"

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

    try {
        const res = await fetch(`${AUTH_API}/users/${userId}`);
        if(!res.ok) throw new Error("Failed to fetch user info");
        return await res.json();
    } catch(err) {
        console.error("Error fetching username: ", err);
        return null;
    }
}