import Navbar from "./components/layout/Navbar";
import Footer from "./components/layout/Footer";

import Hero from "./components/landing/Hero";
import HowItWorks from "./components/landing/HowItWorks";
import DeviceRoles from "./components/landing/DeviceRoles";

function App() {
  return (
    <>
      <Navbar />

      <main>
        <Hero />
        <HowItWorks />
        <DeviceRoles />
      </main>

      <Footer />
    </>
  );
}

export default App;
