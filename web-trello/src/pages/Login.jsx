import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthCard from "../components/ui/AuthCard.jsx";
import Input from "../components/ui/Input.jsx";
import Checkbox from "../components/ui/Checkbox.jsx";
import Button from "../components/ui/Button.jsx";
import { useAuth } from "../modules/auth/AuthContext.jsx";
import { GoogleLogin } from "@react-oauth/google";
import { jwtDecode } from "jwt-decode";
import { motion } from "framer-motion";

export default function Login() {
  const { login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const { state } = useLocation();

  const {
    register,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm({ defaultValues: { remember: true } });

  // --- Config de animación ---
  const LOGO_SRC = "/Logo_transparente_morado.png";
  const duration = 2;
  const startScale = 3.0;
  const endScale = 0.55;
  const endTop = -260;

  async function onSubmit({ email, password, remember }) {
    try {
      await login(email, password, remember);
      navigate(state?.from?.pathname || "/dashboard", { replace: true });
    } catch (err) {
      alert(err.message);
    }
  }

  const handleGoogleSuccess = (credentialResponse) => {
    const decoded = jwtDecode(credentialResponse.credential);
    loginWithGoogle(decoded);
    navigate("/dashboard", { replace: true });
  };

  const handleGoogleError = () => alert("Error al iniciar sesión");

  return (
    <div className="login-page relative min-h-screen flex flex-col items-center justify-center overflow-hidden transition-colors duration-700 ease-in-out">



      {/* Logo animado que sube */}
      <motion.img
        src={LOGO_SRC}
        alt="Logo"
        className="absolute left-1/2 -translate-x-1/2 select-none drop-shadow-2xl z-50 pointer-events-none"
        style={{ top: `calc(40vh - 28px)` }}
        initial={{ scale: startScale, y: "-50vh", opacity: 1 }}
        animate={{ scale: endScale, y: `calc(${endTop}px - 50vh)`, opacity: 1 }}
        transition={{ duration, ease: [0.22, 1, 0.36, 1] }}
      />

      {/* Tarjeta de Login */}
      <motion.div
        className="w-full max-w-md px-4 relative z-10"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: duration * 0.7, duration: 0.45 }}
      >
        <AuthCard
          title="Bienvenido"
          subtitle="Inicia sesión para continuar."
          footer={
            <p className="text-neutral-700 dark:text-neutral-300">
              ¿Aún no tienes una cuenta?{" "}
              <Link
                to="/register"
                className="text-violet-600 dark:text-violet-400 hover:underline font-medium"
              >
                ¡Regístrate!
              </Link>
            </p>
          }
          className="bg-white dark:bg-[#1b1a24] dark:text-white transition-colors duration-300"
        >
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Email"
              type="email"
              name="email"
              placeholder="Introduce tu correo electrónico"
              register={register}
              required
              className="dark:bg-[#242236] dark:border-[#343447] dark:text-neutral-100"
            />
            <Input
              label="Contraseña"
              type="password"
              name="password"
              placeholder="Introduce tu contraseña"
              register={register}
              required
              className="dark:bg-[#242236] dark:border-[#343447] dark:text-neutral-100"
            />

            <div className="flex items-center justify-between">
              <Checkbox
                label="Recordarme"
                name="remember"
                register={register}
                defaultChecked
              />
              <Link
                to="/forgot"
                className="text-sm text-violet-600 dark:text-violet-400 hover:underline"
              >
                He olvidado mi contraseña
              </Link>
            </div>

            <Button
              type="submit"
              variant="primary"
              full
              disabled={isSubmitting}
              className="dark:bg-violet-700 dark:hover:bg-violet-600"
            >
              {isSubmitting ? "Iniciando sesión..." : "Iniciar sesión"}
            </Button>
          </form>

          {/* Botón login con Google */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">
              O usa tu cuenta de Google
            </p>
            <div className="flex justify-center">
              <GoogleLogin
                onSuccess={handleGoogleSuccess}
                onError={handleGoogleError}
              />
            </div>
          </div>
        </AuthCard>
      </motion.div>
    </div>
  );
}
