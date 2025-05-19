import React, { useState } from "react";
import { useFormik } from "formik";
import "../index.css";

export const AuthForm = () => {
  const [isLogin, setIsLogin] = useState(true); // Toggle between login and register

  const formik = useFormik({
    initialValues: {
      email: "",
      password: "",
      nom: "",    
      prenom: "", 
    },
    onSubmit: async (values) => {
      const url = isLogin
        ? "http://localhost:8080/users/login"
        : "http://localhost:8080/users/register";

      const payload = isLogin
        ? { email: values.email, password: values.password }
        : {
            email: values.email,
            password: values.password,
            nom: values.nom,
            prenom: values.prenom,
          };

      try {
        const response = await fetch(url, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          const errData = await response.json();
          throw new Error(errData.message || "Request failed");
        }

        const data = await response.json();
        alert(`${isLogin ? "Login" : "Registration"} successful!`);
        console.log(data);
      } catch (error) {
        alert(error.message);
        console.error(error);
      }
    },
  });

  return (
    <div>
      <h2>{isLogin ? "Login" : "Register"}</h2>
      <form onSubmit={formik.handleSubmit}>
        {!isLogin && (
          <>
            <label htmlFor="nom">Nom</label>
            <input
              id="nom"
              name="nom"
              type="text"
              onChange={formik.handleChange}
              value={formik.values.nom}
            />

            <label htmlFor="prenom">Prénom</label>
            <input
              id="prenom"
              name="prenom"
              type="text"
              onChange={formik.handleChange}
              value={formik.values.prenom}
            />
          </>
        )}

        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          onChange={formik.handleChange}
          value={formik.values.email}
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          onChange={formik.handleChange}
          value={formik.values.password}
        />

        <button type="submit">{isLogin ? "Login" : "Register"}</button>
      </form>

      <button
        style={{ marginTop: "10px" }}
        type="button"
        onClick={() => setIsLogin(!isLogin)}
      >
        {isLogin ? "Create an account" : "Already have an account? Login"}
      </button>
    </div>
  );
};
