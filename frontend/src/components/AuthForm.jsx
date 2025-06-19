import React, { useState } from "react";
import { useFormik } from "formik";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import {
  Button,
  FormControl,
  FormLabel,
  Input,
  Stack,
  Alert,
  Modal,
  ModalDialog,
  Typography,
} from "@mui/joy";

const parseJwt = (token) => {
  try {
    return JSON.parse(atob(token.split(".")[1]));
  } catch {
    return null;
  }
};

export const AuthForm = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    nom: "",
    prenom: "",
  });
  const [error, setError] = useState("");
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [isLogin, setIsLogin] = useState(true);
  const [forgotPassword, setForgotPassword] = useState(false);
  const [resetMessage, setResetMessage] = useState("");
  const [resetError, setResetError] = useState("");

  const formik = useFormik({
    initialValues: {
      dtype: "Utilisateur",
      email: "",
      password: "",
      nom: "",
      prenom: "",
    },
    validate: (values) => {
      const errors = {};
      if (!values.email) {
        errors.email = "Email requis";
      } else if (!/\S+@\S+\.\S+/.test(values.email)) {
        errors.email = "Email invalide";
      }
      if (!forgotPassword && !values.password) errors.password = "Mot de passe requis";
      if (!isLogin && !forgotPassword) {
        if (!values.nom) errors.nom = "Nom requis";
        if (!values.prenom) errors.prenom = "Prénom requis";
      }
      return errors;
    },
    onSubmit: async (values, { setSubmitting, setErrors, resetForm }) => {
      if (forgotPassword) {
        setResetMessage("");
        setResetError("");
        try {
          const response = await fetch("http://localhost:8080/forgot-password", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
            },
            body: JSON.stringify({ email: values.email }),
          });

          const data = await response.json();

          if (!response.ok) {
            throw new Error(data.message || "Erreur lors de la requête");
          }

          setResetMessage(data.message);
          resetForm();
        } catch (error) {
          console.error("Error:", error);
          setResetError(error.message || "Erreur lors de l'envoi de la demande");
        } finally {
          setSubmitting(false);
        }
        return;
      }

      // Logique login / inscription
      const url = isLogin
        ? "http://localhost:8080/users/login"
        : "http://localhost:8080/users/register";

      const payload = isLogin
        ? { email: values.email, password: values.password }
        : {
            dtype: values.dtype,
            email: values.email,
            password: values.password,
            nom: values.nom,
            prenom: values.prenom,
          };

      try {
        const response = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          const errData = await response.json();
          throw new Error(errData.message || "Requête échouée");
        }

        const data = await response.json();

        localStorage.setItem("token", data.token);
        if (data.refreshToken) localStorage.setItem("refreshToken", data.refreshToken);

        const decodedToken = parseJwt(data.token);
        const userRole = decodedToken?.role;

        if (!userRole) {
          alert("Impossible de déterminer le rôle utilisateur");
          return;
        }

        switch (userRole) {
          case "SUPER_ADMIN":
            window.location.href = "/dashboard";
            break;
          case "ADMIN_CLIENT":
            window.location.href = "/dashboard";
            break;
          case "AGENT_INVENTAIRE":
            window.location.href = "/dashboard";
            break;
          case "Utilisateur":
            window.location.href = "/dashboard";
            break;
          default:
            alert("Rôle inconnu : accès refusé");
        }
      } catch (error) {
        setErrors({ general: error.message });
      } finally {
        setSubmitting(false);
      }
    },
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      if (isLogin) {
        const response = await axios.post("http://localhost:8080/users/login", formData);
        const { token, user } = response.data;

        localStorage.setItem("token", token);
        navigate("/dashboard");
      } else {
        const response = await axios.post("http://localhost:8080/users/register", formData);
        setShowSuccessModal(true);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Une erreur est survenue");
    }
  };

  return (
    <div className="min-h-screen flex flex-col md:flex-row">
      <div className="md:w-1/2 bg-blue-900 flex items-center justify-center p-8">
        <div className="text-white text-center max-w-md">
          <h1 className="text-4xl font-extrabold mb-4">Gestion d'Inventaire</h1>
          <p className="mb-6 text-blue-300">
            Gérez efficacement vos stocks et produits avec notre plateforme intuitive.
          </p>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="mx-auto w-48 h-48"
            fill="none"
            viewBox="0 0 64 64"
            stroke="currentColor"
            strokeWidth={2}
          >
            <rect x="10" y="14" width="44" height="36" rx="4" ry="4" />
            <path d="M22 14v-4h20v4" />
            <path d="M18 30h28M18 38h28M18 46h28" />
          </svg>
        </div>
      </div>

      <div className="md:w-1/2 flex items-center justify-center p-8 bg-blue-50">
        <form
          onSubmit={formik.handleSubmit}
          className="w-full max-w-md space-y-6 bg-white rounded-lg shadow-lg p-8"
        >
          {!forgotPassword ? (
            <>
              <h2 className="text-3xl font-extrabold text-center text-blue-900 tracking-wide">
                {isLogin ? "Connexion" : "Inscription"}
              </h2>

              {!isLogin && (
                <label className="block text-blue-900 font-semibold">
                  Type d'utilisateur
                  <select
                    id="dtype"
                    name="dtype"
                    value={formik.values.dtype}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                    className="mt-1 block w-full rounded-md border border-blue-700 px-3 py-2
                      focus:outline-none focus:ring-2 focus:ring-blue-600 focus:border-blue-600
                      bg-white text-blue-900"
                  >
                    <option value="Utilisateur">Utilisateur</option>
                    <option value="AdministrateurClient">Administrateur Client</option>
                    <option value="SuperAdministrateur">Super Administrateur</option>
                    <option value="AgentInventaire">Agent Inventaire</option>
                  </select>
                </label>
              )}

              {!isLogin && (
                <>
                  <label className="block text-blue-900 font-semibold">
                    Nom
                    <input
                      id="nom"
                      name="nom"
                      type="text"
                      placeholder="Nom"
                      onChange={formik.handleChange}
                      onBlur={formik.handleBlur}
                      value={formik.values.nom}
                      className={`mt-1 block w-full rounded-md border px-3 py-2 focus:outline-none ${
                        formik.touched.nom && formik.errors.nom
                          ? "border-blue-600 focus:ring-blue-600"
                          : "border-blue-700 focus:ring-blue-600"
                      } bg-white text-blue-900`}
                    />
                    {formik.touched.nom && formik.errors.nom && (
                      <p className="text-blue-700 text-sm mt-1">{formik.errors.nom}</p>
                    )}
                  </label>

                  <label className="block text-blue-900 font-semibold">
                    Prénom
                    <input
                      id="prenom"
                      name="prenom"
                      type="text"
                      placeholder="Prénom"
                      onChange={formik.handleChange}
                      onBlur={formik.handleBlur}
                      value={formik.values.prenom}
                      className={`mt-1 block w-full rounded-md border px-3 py-2 focus:outline-none ${
                        formik.touched.prenom && formik.errors.prenom
                          ? "border-blue-600 focus:ring-blue-600"
                          : "border-blue-700 focus:ring-blue-600"
                      } bg-white text-blue-900`}
                    />
                    {formik.touched.prenom && formik.errors.prenom && (
                      <p className="text-blue-700 text-sm mt-1">{formik.errors.prenom}</p>
                    )}
                  </label>
                </>
              )}

              <label className="block text-blue-900 font-semibold">
                Email
                <input
                  id="email"
                  name="email"
                  type="email"
                  placeholder="Email"
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  value={formik.values.email}
                  className={`mt-1 block w-full rounded-md border px-3 py-2 focus:outline-none ${
                    formik.touched.email && formik.errors.email
                      ? "border-blue-600 focus:ring-blue-600"
                      : "border-blue-700 focus:ring-blue-600"
                  } bg-white text-blue-900`}
                />
                {formik.touched.email && formik.errors.email && (
                  <p className="text-blue-700 text-sm mt-1">{formik.errors.email}</p>
                )}
              </label>

              <label className="block text-blue-900 font-semibold">
                Mot de passe
                <input
                  id="password"
                  name="password"
                  type="password"
                  placeholder="Mot de passe"
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  value={formik.values.password}
                  className={`mt-1 block w-full rounded-md border px-3 py-2 focus:outline-none ${
                    formik.touched.password && formik.errors.password
                      ? "border-blue-600 focus:ring-blue-600"
                      : "border-blue-700 focus:ring-blue-600"
                  } bg-white text-blue-900`}
                />
                {formik.touched.password && formik.errors.password && (
                  <p className="text-blue-700 text-sm mt-1">{formik.errors.password}</p>
                )}
              </label>

              {formik.errors.general && (
                <p className="text-blue-700 text-center font-semibold">{formik.errors.general}</p>
              )}

              <button
                type="submit"
                disabled={formik.isSubmitting}
                className="w-full bg-blue-900 hover:bg-blue-800 text-white font-extrabold py-3 rounded-md
                  transition duration-300 disabled:opacity-60"
              >
                {isLogin ? "Se connecter" : "S'inscrire"}
              </button>

              {isLogin && (
                <p
                  className="text-blue-900 hover:underline cursor-pointer text-center mt-3 font-semibold"
                  onClick={() => setForgotPassword(true)}
                >
                  Mot de passe oublié ?
                </p>
              )}

              <button
                type="button"
                onClick={() => setIsLogin(!isLogin)}
                className="w-full text-blue-900 hover:underline font-semibold mt-3"
              >
                
              </button>
            </>
          ) : (
            <>
              <h2 className="text-3xl font-extrabold text-center text-blue-900 tracking-wide mb-4">
                Réinitialisation du mot de passe
              </h2>

              <label className="block text-blue-900 font-semibold">
                Email
                <input
                  id="email"
                  name="email"
                  type="email"
                  placeholder="Votre adresse email"
                  onChange={formik.handleChange}
                  onBlur={formik.handleBlur}
                  value={formik.values.email}
                  className={`mt-1 block w-full rounded-md border px-3 py-2 focus:outline-none ${
                    formik.touched.email && formik.errors.email
                      ? "border-blue-600 focus:ring-blue-600"
                      : "border-blue-700 focus:ring-blue-600"
                  } bg-white text-blue-900`}
                />
                {formik.touched.email && formik.errors.email && (
                  <p className="text-blue-700 text-sm mt-1">{formik.errors.email}</p>
                )}
              </label>

              {resetError && <p className="text-red-600 text-center">{resetError}</p>}
              {resetMessage && <p className="text-green-600 text-center">{resetMessage}</p>}

              <button
                type="submit"
                disabled={formik.isSubmitting}
                className="w-full bg-blue-900 hover:bg-blue-800 text-white font-extrabold py-3 rounded-md
                  transition duration-300 disabled:opacity-60"
              >
                Envoyer le lien de réinitialisation
              </button>

              <button
                type="button"
                onClick={() => {
                  setForgotPassword(false);
                  setResetError("");
                  setResetMessage("");
                  formik.resetForm();
                }}
                className="w-full text-blue-900 hover:underline font-semibold mt-3"
              >
                Retour à la connexion
              </button>
            </>
          )}
        </form>
      </div>

      <Modal
        open={showSuccessModal}
        onClose={() => {
          setShowSuccessModal(false);
          navigate("/login");
        }}
      >
        <ModalDialog aria-labelledby="success-modal" size="md">
          <Typography level="h4" id="success-modal">
            Inscription réussie !
          </Typography>
          <Typography level="body-md">
            Un email de vérification a été envoyé à {formData.email}.
            <br />
            Veuillez vérifier votre boîte de réception et cliquer sur le lien pour activer votre compte.
          </Typography>
          <Button
            onClick={() => {
              setShowSuccessModal(false);
              navigate("/login");
            }}
            sx={{ mt: 2 }}
          >
            Aller à la page de connexion
          </Button>
        </ModalDialog>
      </Modal>
    </div>
  );
};

export default AuthForm;
