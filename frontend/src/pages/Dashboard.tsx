import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import Sidebar from "../components/SideBar";
import TaskList from "../components/TaskList";
import TaskModal from "../components/TaskModal";
import FriendsModal from "../components/FriendsModal";
import { fetchUserProfile } from "../utils/fetchUserProfile"
import type { UserProfile } from "../utils/fetchUserProfile";
import { API_URL } from "../config/api";
import { connectNotifications, disconnectNotifications } from "../utils/websocket";
import type { Notification } from "../utils/websocket";

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
    const [friends, setFriends] = useState<{id: string; username: string}[]>([]);
    const [notifications, setNotifications] = useState<Notification[]>([]);
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
        const token = localStorage.getItem("token");
        const myId = JSON.parse(atob(token!.split(".")[1])).sub;
        const updated = [{ id: myId, username: "Me" }, ...data];
        setFriends(updated);        
        }
    };

    const createTask = async (): Promise<boolean> => {
        if(!title.trim()) return false;
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
            console.error("Task creation failed: ", res.statusText);
            return false;
        }
        
    };

    const updateTask = async(): Promise<boolean> => {
        if(!editingTask) return false;

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
            console.error("Task update failed", res.statusText);
            return false;
        }
    }

    const markDone = async ( id: string, currentStatus: string) => {
        const newStatus = currentStatus === "DONE" ? "TODO": "DONE";
        await fetch (`${API}/tasks/${id}/status`, {
            method: "PATCH",
            headers,
            body: JSON.stringify({status: newStatus}),
        })
        fetchTasks()
    }

    const deleteTask = async (id: string) => {
        await fetch(`${API}/tasks/${id}`, {method: "DELETE", headers})
        fetchTasks()
    }

    useEffect(() => {
        fetchTasks();
        fetchNotifications();
        fetchUserProfile().then(profile => {
            setUser(profile);
            if (profile) {
            setFriends(prev => [
                ...prev.filter(f => f.id !== profile.id),
                { id: profile.id, username: profile.username }
            ]);
            }
        });
        }, []);

    useEffect(() => {
        connectNotifications((notification) => {
            console.log("New notification received:", notification);
            setNotifications(prev => [notification, ...prev]);
        });
        return () => disconnectNotifications();
        }, []);


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
      <div className="flex flex-1 p-6 space-x-6">
        <Sidebar 
        username={user?.username || "User"}
        bio = {user?.bio}
        profilePic = {user?.profilePic} 
        onEditProfile={() => {}}/>
        <main className="flex-1 bg-gray-800 p-6 rounded-2xl shadow-lg">
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
    </div>
  );
}
