    interface NavbarProps {
        onCreateTaskClick: () => void;
    }
export default function Navbar({onCreateTaskClick}:NavbarProps){

    const handleLogout = () => {
        localStorage.removeItem("token")
        window.location.href = "/"
    }


    return (
        <nav className="flex items-center justify-between bg-white shadow px-6 py-3">
            <div className="text-2xl font-bold text-red-600">SayLess</div>
            <div className="flex space-x-4">
                <button onClick={onCreateTaskClick} className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600">
                    Create Task</button>
                <button className="bg-gray-200 text-gray-700 px-4 py-2 rounded-lg hover: bg-gray-300">
                    Friends</button>
                <button onClick={handleLogout} className="bg-gray-200 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-300">
                    Logout</button>
            </div>
        </nav>
    );
}