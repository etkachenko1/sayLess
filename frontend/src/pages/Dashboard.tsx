import { useState, useEffect } from "react";
import Navbar from "../components/Navbar";
import Sidebar from "../components/SideBar";
import TaskList from "../components/TaskList";
import TaskModal from "../components/TaskModal";
import { fetchUserProfile } from "../utils/fetchUserProfile"
import type { UserProfile } from "../utils/fetchUserProfile";
interface Task {
    id: string
    title: string
    description: string
    status: string
    assignedTo: string
    createdBy: string
}

const API = "http://localhost:8082"

export default function Dashboard(){
    const [tasks, setTasks] = useState<Task[]>([])
    const [title, setTitle] = useState("")
    const [description, setDescription] = useState("")
    const [loading, setLoading] = useState(false)
    const [user, setUser] = useState<UserProfile | null>(null)
    const [showTaskModal, setShowTaskModal] = useState(false);
    const [assignedTo, setAssignedTo ] = useState(user?.username || "")
    const [deadline, setDeadline] = useState("")
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

    const markDone = async ( id: string) => {
        await fetch (`${API}/tasks/${id}/status`, {
            method: "PATCH",
            headers,
            body: JSON.stringify({status: "DONE"}),
        })
        fetchTasks()
    }

    const deleteTask = async (id: string) => {
        await fetch(`${API}/tasks/${id}`, {method: "DELETE", headers})
        fetchTasks()
    }

    useEffect(() => {
        fetchTasks()
        fetchUserProfile().then(setUser)
    }, [])


    return (
         <div className="min-h-screen flex flex-col bg-gray-900 text-gray-100">
      <Navbar onCreateTaskClick= {() => setShowTaskModal(true)}/>
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
            onMarkDone={markDone}
            onDelete={deleteTask}
          />
        </main>
      </div>

      <TaskModal 
      isOpen = {showTaskModal}
      onClose ={() =>setShowTaskModal(false)}
      title = {title}
      description= {description}
      assignedTo={assignedTo}
      deadline= {deadline}
      onTitleChange={setTitle}
      onDescriptionChange={setDescription}
      onAssignedToChange={setAssignedTo}
      onDeadlineChange={setDeadline}
      onSubmit={createTask}
      />
    </div>
  );
}
