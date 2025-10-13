import { useState, useEffect } from "react";

interface Friend {
  id: string;
  requesterId: string;
  requesterName: string;
  receiverId: string;
  receiverName: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED";
  createdAt: string;
}

interface UserResult {
  id: string;
  username: string;
}

interface FriendsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const API = "http://localhost:8083";


export default function FriendsModal({ isOpen, onClose }: FriendsModalProps) {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [search, setSearch] = useState("");
  const [searchResults, setSearchResults] = useState<UserResult[]>([]);
  const token = localStorage.getItem("token");
  const myId = JSON.parse(atob(token!.split(".")[1])).sub;
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };

  const fetchFriends = async () => {
    const res = await fetch(`${API}/friends`, { headers });
    if (res.ok) {
        const data: Friend[] = await res.json();
        //sort newest first
        data.sort((a,b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setFriends(data);
    }
  };
      const searchUsers = async ()=>{
        if(search.trim().length < 2) {
            setSearchResults([]);
            return;
        }
        const res = await fetch(`${API}/friends/search?username=${search}`, {headers });
        if(res.ok) {
            const users = await res.json();
            setSearchResults(users);
        }    
    };

  const sendRequest = async (receiverId: string) => {
    const res = await fetch(`${API}/friends/request?receiverId=${receiverId}`, {
      method: "POST",
      headers,
    });
    if (res.ok) fetchFriends();
  };

  const acceptRequest = async (requesterId: string) => {
    const res = await fetch(`${API}/friends/accept?requesterId=${requesterId}`, {
      method: "POST",
      headers,
    });
    if (res.ok) fetchFriends();
  };

  const removeFriend = async (friendId: string) => {
    const res = await fetch(`${API}/friends/remove?friendId=${friendId}`, {
      method: "DELETE",
      headers,
    });
    if (res.ok) fetchFriends();
  };

  useEffect(() => {
    if (isOpen) fetchFriends();
  }, [isOpen]);

  useEffect(() => {
    searchUsers();
  }, [search]);

if (!isOpen) return null;
  
  const pending = friends.filter(f => f.status === "PENDING");
  const accepted = friends.filter(f => f.status === "ACCEPTED");
  const declined = friends.filter(f => f.status === "DECLINED");

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-gray-800 rounded-xl shadow-lg w-full max-w-lg p-6">
        <h2 className="text-xl font-bold mb-4 text-center text-red-700">Friends</h2>

        <input
          type="text"
          placeholder="Search users..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full mb-4 p-2 rounded-lg bg-gray-700 text-gray-300 border border-gray-600"
        />
        {searchResults.length > 0 && (
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-blue-400 mb-2">Search Results</h3>
            {searchResults.map((user) => (
              <div key={user.id} className="flex justify-between items-center bg-gray-700 rounded-lg p-2">
                <span>{user.username}</span>
                <button
                  onClick={() => sendRequest(user.id)}
                  className="bg-blue-600 text-white px-2 py-1 rounded hover:bg-blue-500"
                >
                  Add
                </button>
              </div>
            ))}
          </div>
        )}
        <div className="max-h-80 overflow-y-auto space-y-4">
        {pending.length > 0 && (
  <div>
    <h3 className="text-sm font-semibold text-yellow-400 mb-2">Pending Requests</h3>
    {pending.map(f => {
      const isRequester = f.requesterId === myId;
      return (
        <div key={f.id} className="flex justify-between items-center bg-gray-700 rounded-lg p-2">
          <span>
            {isRequester
              ? `Request sent to ${f.receiverName}`
              : `Friend request from ${f.requesterName}`}
          </span>

          {!isRequester && (
            <button
              onClick={() => acceptRequest(f.requesterId)}
              className="bg-green-600 text-white px-2 py-1 rounded hover:bg-green-500"
            >
              Accept
            </button>
          )}
        </div>
      );
    })}
  </div>
)}


          {accepted.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold text-green-400 mb-2">Friends</h3>
              {accepted.map(f => (
                <div key={f.id} className="flex justify-between items-center bg-gray-700 rounded-lg p-2">
                  <span>{f.requesterName}</span>
                  <button
                    onClick={() => removeFriend(f.requesterId)}
                    className="bg-red-600 text-white px-2 py-1 rounded hover:bg-red-500"
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}

          {declined.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold text-gray-400 mb-2">Declined</h3>
              {declined.map(f => (
                <div key={f.id} className="flex justify-between items-center bg-gray-700 rounded-lg p-2">
                  <span>{f.requesterId} → {f.receiverId}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="text-center mt-4">
          <button
            onClick={onClose}
            className="text-gray-400 hover:underline"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}