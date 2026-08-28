import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import Sidebar from "../components/SideBar";
import TaskList from "../components/TaskList";
import TaskModal from "../components/TaskModal";
import FriendsModal from "../components/FriendsModal";
import ProfileModal from "../components/ProfileModal";
import { fetchUserProfile } from "../utils/fetchUserProfile"
import type { UserProfile } from "../utils/fetchUserProfile";
import { API_URL } from "../config/api";
import { connectRealtime, disconnectRealtime } from "../utils/websocket";
import type { Notification, TaskEvent, TaskSnapshot } from "../utils/websocket";

interface Task {
    id: string
    title: string
    description: string
    status: string
    deadline: string
    assignedToId: string
    createdById: string
    createdByName: string
    assignedToName: string
    updatedAt?: string
}

function taskSnapshotToTask(snapshot: TaskSnapshot): Task {
    return {
        id: snapshot.taskId,
        title: snapshot.title,
        description: snapshot.description,
        status: snapshot.status,
        deadline: snapshot.deadline,
        assignedToId: snapshot.assignedToId,
        assignedToName: snapshot.assignedToName,
        createdById: snapshot.createdById,
        createdByName: snapshot.createdByName,
        updatedAt: snapshot.updatedAt,
    };
}

const API = API_URL

export default function Dashboard(){
    const [tasks, setTasks] = useState<Task[]>([])
    const [title, setTitle] = useState("")
    const [description, setDescription] = useState("")
    const [loading, setLoading] = useState(false)
    const [user, setUser] = useState<UserProfile | null>(null)
    const [showTaskModal, setShowTaskModal] = useState(false);
    const [assignedTo, setAssignedTo ] = useState(user?.username || "")
    const [deadline, setDeadline] = useState("")
    const [editingTask, setEditingTask] = useState<Task | null> (null);
    const [ShowFriendsModal, setShowFriendsModal] = useState(false);
    const [showProfileModal, setShowProfileModal] = useState(false);
    const [friends, setFriends] = useState<{id: string; username: string}[]>([]);
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [taskError, setTaskError] = useState<string>("");
    const token = localStorage.getItem("token")

    const headers = {
        "Content-Type" : "application/json",
        Authorization: `Bearer ${token}`,
    }

    const fetchTasks = async () => {
        setLoading(true)
        const res = await fetch(`${API}/tasks`, {headers});
        console.log("fetch /tasks response: ", res.status, res.statusText);
        const text = await res.text();
        if(res.ok) {
            setTasks(JSON.parse(text))
        }
        else {
            console.error("Failed to load tasks")
        }
        setLoading(false)
    }

    const fetchNotifications = async () => {
        const res = await fetch(`${API}/notifications`, { headers });
        if (res.ok) {
            setNotifications(await res.json());
        }
    };

    const markAllNotificationsRead = async () => {
        const unread = notifications.filter(n => !n.read);
        if (unread.length === 0) return;
        setNotifications(prev => prev.map(n => ({ ...n, read: true })));
        await Promise.all(unread.map(n =>
            fetch(`${API}/notifications/${n.id}/read`, { method: "PATCH", headers })
        ));
    };

    const deleteNotification = async (id: string) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
        await fetch(`${API}/notifications/${id}`, { method: "DELETE", headers });
    };

    const clearAllNotifications = async () => {
        const all = notifications;
        if (all.length === 0) return;
        setNotifications([]);
        await Promise.all(all.map(n =>
            fetch(`${API}/notifications/${n.id}`, { method: "DELETE", headers })
        ));
    };

    const fetchFriends = async () => {
        const res = await fetch(`${API_URL}/friends/accepted`, { headers });
        if (res.ok) {
        const data = await res.json();
        const myId = token ? JSON.parse(atob(token.split(".")[1])).sub : null;
        const updated = [{ id: myId, username: "Me" }, ...data];
        setFriends(updated);
        }
    };

    const parseErrorMessage = async (res: Response, fallback: string): Promise<string> => {
        try {
            const data = await res.json();
            return data.error || fallback;
        } catch {
            return fallback;
        }
    };

    const createTask = async (): Promise<boolean> => {
        if(!title.trim()) return false;
        setTaskError("");
        const res = await fetch(`${API}/tasks`, {
            method: "POST",
            headers,
            body: JSON.stringify({title, description, assignedTo: assignedTo || null, deadline}),

        });
        if(res.ok){
            await fetchTasks();
            setTitle("")
            setDescription("")
            return true;
        }
        else {
            setTaskError(await parseErrorMessage(res, "Failed to create task"));
            return false;
        }

    };

    const updateTask = async(): Promise<boolean> => {
        if(!editingTask) return false;
        setTaskError("");

        const res= await fetch(`${API}/tasks/${editingTask.id}`, {
            method: "PUT",
            headers,
            body: JSON.stringify({
                title,
                description,
                deadline,
                assignedTo: assignedTo || null,

            })
        })
        if(res.ok) {
            await fetchTasks();
            setEditingTask(null);
            setShowTaskModal(false);
            setTitle("");
            setDescription("");
            setDeadline("");
            return true;
        } else {
            setTaskError(await parseErrorMessage(res, "Only the task's creator can edit it"));
            return false;
        }
    }

    const markDone = async ( id: string, currentStatus: string) => {
        setTaskError("");
        const newStatus = currentStatus === "DONE" ? "TODO": "DONE";
        const res = await fetch (`${API}/tasks/${id}/status`, {
            method: "PATCH",
            headers,
            body: JSON.stringify({status: newStatus}),
        })
        if (!res.ok) {
            setTaskError(await parseErrorMessage(res, "Failed to update task status"));
        }
        fetchTasks()
    }

    const deleteTask = async (id: string) => {
        setTaskError("");
        const res = await fetch(`${API}/tasks/${id}`, {method: "DELETE", headers})
        if (!res.ok) {
            setTaskError(await parseErrorMessage(res, "Only the task's creator can delete it"));
        }
        fetchTasks()
    }

    const updateProfile = async (bio: string, profilePic: string): Promise<string | null> => {
        const res = await fetch(`${API}/users/me`, {
            method: "PUT",
            headers,
            body: JSON.stringify({ bio: bio || null, profilePic: profilePic || null }),
        });
        const data = await res.json();
        if (!res.ok) {
            return data.error || "Failed to update profile";
        }
        setUser(data);
        return null;
    }

    useEffect(() => {
        if (!token) {
            window.location.href = "/";
            return;
        }
        fetchTasks();
        fetchNotifications();
        fetchUserProfile().then(profile => {
            if (!profile) {
                window.location.href = "/";
                return;
            }
            setUser(profile);
            setFriends(prev => [
                ...prev.filter(f => f.id !== profile.id),
                { id: profile.id, username: profile.username }
            ]);
        });
        }, []);

    useEffect(() => {
        connectRealtime(
            (notification) => {
                setNotifications(prev => [notification, ...prev]);
            },
            (event: TaskEvent) => {
                if (event.type === "removed") {
                    setTasks(prev => prev.filter(t => t.id !== event.taskId));
                    return;
                }
                const incoming = taskSnapshotToTask(event.task);
                setTasks(prev => {
                    const existingIndex = prev.findIndex(t => t.id === incoming.id);
                    if (existingIndex === -1) {
                        return [...prev, incoming];
                    }
                    const existing = prev[existingIndex];
                    if (existing.updatedAt && incoming.updatedAt && existing.updatedAt >= incoming.updatedAt) {
                        return prev;
                    }
                    const next = [...prev];
                    next[existingIndex] = incoming;
                    return next;
                });
            },
            () => {
                fetchTasks();
            }
        );
        return () => disconnectRealtime();
        }, []);

    useEffect(() => {
        const handlePageShow = (event: PageTransitionEvent) => {
            if (event.persisted && !localStorage.getItem("token")) {
                window.location.href = "/";
            }
        };
        window.addEventListener("pageshow", handlePageShow);
        return () => window.removeEventListener("pageshow", handlePageShow);
    }, []);

    if (!token || !user) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-900 text-gray-100">
                Loading...
            </div>
        );
    }

    return (
         <div className="min-h-screen flex flex-col bg-gray-900 text-gray-100">
      <Navbar 
      onCreateTaskClick= {() => {
        fetchFriends();
        setEditingTask(null);
        setTitle('');
        setDescription('');
        setAssignedTo(user?.id || '');
        setDeadline(new Date(new Date().setHours(23,59,0,0)).toISOString());
        setShowTaskModal(true)}}
        onFriendsClick={()=> setShowFriendsModal(true)}
        notifications={notifications}
        onMarkAllNotificationsRead={markAllNotificationsRead}
        onDeleteNotification={deleteNotification}
        onClearAllNotifications={clearAllNotifications}
        />
      <div className="flex flex-col md:flex-row flex-1 p-6 gap-6">
        <Sidebar
        username={user?.username || "User"}
        bio = {user?.bio}
        profilePic = {user?.profilePic}
        onEditProfile={() => setShowProfileModal(true)}/>
        <main className="flex-1 min-w-0 bg-gray-800 p-6 rounded-2xl shadow-lg">
          {taskError && (
            <div className="mb-4 p-2 text-sm text-red-400 bg-red-900/30 border border-red-700 rounded">
              {taskError}
            </div>
          )}
          <TaskList
            tasks={tasks}
            loading={loading}
            onToggleStatus={markDone}
            onDelete={deleteTask}
            onEdit = {(task) => {
                setEditingTask(task);
                setTitle(task.title);
                setDescription(task.description);
                setDeadline(task.deadline|| "");
                setAssignedTo(task.assignedToId);
                setShowTaskModal(true);
            }}
          />
        </main>
      </div>

      <TaskModal 
      isOpen = {showTaskModal}
      onClose ={() => {setShowTaskModal(false); setEditingTask(null);}}
      title = {title}
      description= {description}
      assignedTo={assignedTo}
      deadline= {deadline}
      onTitleChange={setTitle}
      onDescriptionChange={setDescription}
      onAssignedToChange={setAssignedTo}
      onDeadlineChange={setDeadline}
      onSubmit={editingTask? updateTask : createTask}
      isEditing = {!!editingTask}
      friends={friends}
      />

      <FriendsModal
      isOpen={ShowFriendsModal}
      onClose={() => setShowFriendsModal(false)}/>

      <ProfileModal
      isOpen={showProfileModal}
      onClose={() => setShowProfileModal(false)}
      bio={user?.bio}
      profilePic={user?.profilePic}
      onSubmit={updateProfile}
      />
    </div>
  );
}
