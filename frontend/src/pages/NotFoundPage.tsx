import { Link } from 'react-router-dom';
export function NotFoundPage() {
  return (
    <div className="grid min-h-screen place-items-center bg-slate-50 p-6 text-center">
      <div>
        <p className="text-6xl font-black text-teal-600">404</p>
        <h1 className="mt-4 text-2xl font-bold">Trang không tồn tại</h1>
        <Link className="mt-6 inline-block font-semibold text-teal-700" to="/">
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
