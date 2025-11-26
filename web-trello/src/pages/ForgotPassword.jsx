import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import AuthCard from "../components/ui/AuthCard.jsx";
import Input from "../components/ui/Input.jsx";
import Button from "../components/ui/Button.jsx";
import { useAuth } from "../modules/auth/AuthContext.jsx";

export default function ForgotPassword() {
  const { resetPassword } = useAuth();
  const { register, handleSubmit, formState: { isSubmitting } } = useForm();

  async function onSubmit({ email, newPassword }) {
    try {
      await resetPassword(email, newPassword);
      alert("Password updated.");
    } catch (err) {
      alert(err.message);
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-[var(--color-brand-50)] dark:bg-[var(--color-brand-25)] transition-colors duration-300 px-4">
      <AuthCard
        title="Restaurar contraseña"
        subtitle="Introduce tu email y una nueva contraseña."
        footer={<p>Vuele a <Link to="/login" className="text-violet-600 hover:underline">Iniciar sesión</Link></p>}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input label="Email" type="email" name="email" placeholder="Introduce tu email" register={register} />
          <Input label="Nueva contraseña" type="password" name="newPassword" placeholder="Introduce la nueva contraseña" register={register} />
          <Button type="submit" variant="primary" full disabled={isSubmitting}>
            {isSubmitting ? "Guardando" : "Actualizar contraseña"}
          </Button>
        </form>
      </AuthCard>
    </main>
  );
}
