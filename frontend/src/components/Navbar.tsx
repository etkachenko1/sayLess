import logo from "../assets/sayless-logo.png"
interface NavbarProps {
    onCreateTaskClick: () => void;
}
export default function Navbar({onCreateTaskClick}:NavbarProps){

    const handleLogout = () => {
        localStorage.removeItem("token")
        window.location.href = "/"
    }


    return (
        <nav className="flex items-center justify-between bg-gray-900 shadow px-6 py-3">
            <div className="flex items-center space-x-3">
                <span className="text-2xl font-bold text-red-700 tracking-wide"> SayLess </span>
                <img src = {logo} alt = "SayLess Logo" className="h-10 w-auto"/>
             </div>
            <div className="flex space-x-4">
                <button onClick={onCreateTaskClick} className="bg-red-700 text-white px-4 py-2 rounded-lg hover:bg-red-600">
                    Create Task</button>
                <button className="bg-gray-700 text-gray-200 px-4 py-2 rounded-lg hover: bg-gray-300">
                    Friends</button>
                <button onClick={handleLogout} className="bg-gray-700 text-gray-200 px-4 py-2 rounded-lg hover:bg-gray-600">
                    Logout</button>
            </div>
        </nav>
    );
}