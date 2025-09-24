import React, { useState } from "react"
import type { ChangeEvent } from "react"
import logo from "./assets/sayless-logo.png"



interface RegisterData {
    username: string
    email: string
    password: string
}
interface LoginData {
    username: string
    password: string
}

function Auth() {
    const [mode, setMode] = useState<"register" | "login">("login")
    const [registerForm, setRegisterForm] = useState<RegisterData>({
        username:"",
        email:"",
        password: "",
    })
   const [loginForm, setLoginForm] = useState<LoginData>({
        username:"",
        password: "",
    })
    const [token, setToken] = useState<string>("")
    const [error, setError] = useState<string>("")

    const handleChangeRegister = (e: ChangeEvent<HTMLInputElement>) =>{
        const {name, value} = e.target //pulls the name and value of the input
        setRegisterForm((prev) => ({...prev,[name]:value})) // copies previous state, replaces the fiels matvhing the input's name
    }
    const handleChangeLogin = (e: ChangeEvent<HTMLInputElement>) =>{
        const {name, value} = e.target //pulls the name and value of the input
        setLoginForm((prev) => ({...prev,[name]:value})) // copies previous state, replaces the fiels matvhing the input's name
    }

    const register = async () => {
        setError("")
        try {
            //send HTTP Post requesr to backend
            const res = await fetch("http://localhost:8081/auth/register", {
                method: "POST",
                headers: {"Content-Type" : "appllication/json"},
                body: JSON.stringify(registerForm),
            }) 
            const data = await res.json() //parses the response body from the backend into JS object
            if(!res.ok) {
                const msg = await res.text()
                setError(msg)
                return
            }
            alert(data.message || "Registered Successfully!")
            setMode("login")
        }
        catch(err) {
            console.error("Register error:", err)
            setError("Something went wrong. Try again later.")
        }
    }
    
    const login = async () => {
        setError("")
        try {
            const res = await fetch("http://localhost:8081/auth/login", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(loginForm),
            })
            
            const data = await res.json()
            //check if backend returned a JWT token
            if(res.ok &&data.token) {
                setToken(data.token) //store token  token in component state
                localStorage.setItem("token", data.token) //token persistamnce
            } else {
                alert("Login failed: " + JSON.stringify(data))
                setError("Invalid username or password")
            }

        }catch (err) {
            console.error("Login error:", err)
            setError("Something went wrong. Please try again later")
        }
    }

    return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-r from-pink-500 via-purple-500 to-blue-500">
      <div className="w-full max-w-md bg-white rounded-xl shadow-lg p-8">
        
        {/* Logo */}
        <div className="flex justify-center mb-6">
          <img src={logo} alt="SayLess logo" className="h-20" />
        </div>

        {/* Toggle buttons */}
        <div className="flex justify-center mb-6">
          <button
            onClick={() => setMode("login")}
            className={`px-4 py-2 rounded-l-lg ${
              mode === "login" ? "bg-pink-500 text-white" : "bg-gray-200"
            }`}
          >
            Sign In
          </button>
          <button
            onClick={() => setMode("register")}
            className={`px-4 py-2 rounded-r-lg ${
              mode === "register" ? "bg-pink-500 text-white" : "bg-gray-200"
            }`}
          >
            Sign Up
          </button>
        </div>

        {/* Error message */}
        {error && (
          <div className="mb-4 p-2 text-sm text-red-700 bg-red-100 rounded">
            {error}
          </div>
        )}

        {/* Login Form */}
        {mode === "login" && (
          <>
            <input
              name="username"
              placeholder="Username"
              value={loginForm.username}
              onChange={handleChangeLogin}
              className="w-full p-3 mb-3 border rounded"
            />
            <input
              name="password"
              type="password"
              placeholder="Password"
              value={loginForm.password}
              onChange={handleChangeLogin}
              className="w-full p-3 mb-4 border rounded"
            />
            <button
              onClick={login}
              className="w-full bg-green-500 text-white py-2 rounded-lg hover:bg-green-600 transition"
            >
              Sign In
            </button>
          </>
        )}

        {/* Register Form */}
        {mode === "register" && (
          <>
            <input
              name="username"
              placeholder="Username"
              value={registerForm.username}
              onChange={handleChangeRegister}
              className="w-full p-3 mb-3 border rounded"
            />
            <input
              name="email"
              placeholder="Email"
              value={registerForm.email}
              onChange={handleChangeRegister}
              className="w-full p-3 mb-3 border rounded"
            />
            <input
              name="password"
              type="password"
              placeholder="Password"
              value={registerForm.password}
              onChange={handleChangeRegister}
              className="w-full p-3 mb-4 border rounded"
            />
            <button
              onClick={register}
              className="w-full bg-pink-500 text-white py-2 rounded-lg hover:bg-pink-600 transition"
            >
              Sign Up
            </button>
          </>
        )}

        {/* Show JWT token */}
        {token && (
          <div className="mt-6 p-4 bg-gray-100 rounded">
            <h3 className="font-bold text-sm">JWT Token:</h3>
            <p className="text-xs break-all">{token}</p>
          </div>
        )}
      </div>
    </div>
  )
}

export default Auth