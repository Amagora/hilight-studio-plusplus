namespace HiLightManager;

static class Program
{
    /// <summary>
    ///  The main entry point for the application.
    /// </summary>
    [STAThread]
    static void Main()
    {
        // To customize application configuration such as set high DPI settings or default font,
        // see https://aka.ms/applicationconfiguration.
        ApplicationConfiguration.Initialize();
        Application.SetUnhandledExceptionMode(UnhandledExceptionMode.CatchException);
        Application.ThreadException += (s, e) =>
        {
            MessageBox.Show($"Application error: {e.Exception.Message}", "HiLight Manager Error", MessageBoxButtons.OK, MessageBoxIcon.Warning);
        };
        AppDomain.CurrentDomain.UnhandledException += (s, e) =>
        {
            if (e.ExceptionObject is Exception ex)
            {
                MessageBox.Show($"Application error: {ex.Message}", "HiLight Manager Error", MessageBoxButtons.OK, MessageBoxIcon.Warning);
            }
        };

        Application.Run(new MainForm());
    }    
}