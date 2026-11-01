function Footer() {
  return (
    <footer className="border-t border-gray-200 bg-white px-6 py-8">
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 sm:flex-row">
        {/* FrameHost branding */}
        <a
          href="/"
          className="text-xl font-bold tracking-tight text-gray-900"
          aria-label="FrameHost home"
        >
          FrameHost<span className="text-blue-600">.</span>
        </a>

        {/* Project tagline */}
        <p className="text-center text-sm text-gray-500">
          One Host. Every Screen. Total Control.
        </p>

        {/* GitHub repository */}
        <a
          href="https://github.com/vivekkushwahaofficial/FrameHost"
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm font-medium text-gray-600 transition-colors hover:text-blue-600"
        >
          GitHub
        </a>
      </div>

      {/* Copyright */}
      <div className="mx-auto mt-6 max-w-7xl border-t border-gray-100 pt-6 text-center">
        <p className="text-xs text-gray-500">
          © {new Date().getFullYear()} FrameHost. Open-source project.
        </p>
      </div>
    </footer>
  );
}

export default Footer;
