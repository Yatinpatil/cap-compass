$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$assetDir = Join-Path $projectRoot 'play-store'
New-Item -ItemType Directory -Force -Path $assetDir | Out-Null

function Fill-RoundedRectangle {
    param(
        [System.Drawing.Graphics]$Graphics,
        [System.Drawing.Brush]$Brush,
        [System.Drawing.Rectangle]$Rectangle,
        [int]$Radius
    )
    $diameter = [Math]::Min($Radius * 2, [Math]::Min($Rectangle.Width, $Rectangle.Height))
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddArc($Rectangle.X, $Rectangle.Y, $diameter, $diameter, 180, 90)
    $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Y, $diameter, $diameter, 270, 90)
    $path.AddArc($Rectangle.Right - $diameter, $Rectangle.Bottom - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($Rectangle.X, $Rectangle.Bottom - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    $Graphics.FillPath($Brush, $path)
    $path.Dispose()
}

$sourceIcon = [System.Drawing.Image]::FromFile((Join-Path $projectRoot 'assets\icon-only.png'))
$icon = [System.Drawing.Bitmap]::new(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$iconGraphics = [System.Drawing.Graphics]::FromImage($icon)
$iconGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$iconGraphics.DrawImage($sourceIcon, 0, 0, 512, 512)
$icon.Save((Join-Path $assetDir 'icon-512.png'), [System.Drawing.Imaging.ImageFormat]::Png)

# Keep the pre-Android 8 launcher assets aligned with the Play listing icon.
$nativeResDir = Join-Path $projectRoot 'android\app\src\main\res'
$densitySizes = @{
    'mipmap-ldpi' = 36
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}
foreach ($density in $densitySizes.Keys) {
    $size = $densitySizes[$density]
    $densityDir = Join-Path $nativeResDir $density
    New-Item -ItemType Directory -Force -Path $densityDir | Out-Null

    $launcher = [System.Drawing.Bitmap]::new($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $launcherGraphics = [System.Drawing.Graphics]::FromImage($launcher)
    $launcherGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $launcherGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $launcherGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $launcherGraphics.DrawImage($icon, 0, 0, $size, $size)
    $launcher.Save((Join-Path $densityDir 'ic_launcher.png'), [System.Drawing.Imaging.ImageFormat]::Png)

    $launcherGraphics.Clear([System.Drawing.Color]::Transparent)
    $circlePath = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $circlePath.AddEllipse(0, 0, $size, $size)
    $launcherGraphics.SetClip($circlePath)
    $launcherGraphics.DrawImage($icon, 0, 0, $size, $size)
    $launcher.Save((Join-Path $densityDir 'ic_launcher_round.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $launcherGraphics.Dispose()
    $circlePath.Dispose()
    $launcher.Dispose()

    $adaptiveSize = [int]($size * 2.25)
    $adaptiveBackground = [System.Drawing.Bitmap]::new($adaptiveSize, $adaptiveSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $adaptiveBackgroundGraphics = [System.Drawing.Graphics]::FromImage($adaptiveBackground)
    $adaptiveBackgroundGraphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#124c3c'))
    $adaptiveBackground.Save((Join-Path $densityDir 'ic_launcher_background.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $adaptiveBackgroundGraphics.Dispose()
    $adaptiveBackground.Dispose()

    $adaptiveForeground = [System.Drawing.Bitmap]::new($adaptiveSize, $adaptiveSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $adaptiveForegroundGraphics = [System.Drawing.Graphics]::FromImage($adaptiveForeground)
    $adaptiveForegroundGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $adaptiveForegroundGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $adaptiveForegroundGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $adaptiveForegroundGraphics.DrawImage($icon, 0, 0, $adaptiveSize, $adaptiveSize)
    $adaptiveForeground.Save((Join-Path $densityDir 'ic_launcher_foreground.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $adaptiveForegroundGraphics.Dispose()
    $adaptiveForeground.Dispose()
}

$iconGraphics.Dispose()
$icon.Dispose()
$sourceIcon.Dispose()

$width = 1024
$height = 500
$bitmap = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

$green = [System.Drawing.ColorTranslator]::FromHtml('#124c3c')
$greenDeep = [System.Drawing.ColorTranslator]::FromHtml('#0a352b')
$cream = [System.Drawing.ColorTranslator]::FromHtml('#f4f0e6')
$surface = [System.Drawing.ColorTranslator]::FromHtml('#fffdf8')
$ink = [System.Drawing.ColorTranslator]::FromHtml('#17251f')
$muted = [System.Drawing.ColorTranslator]::FromHtml('#55635b')
$saffron = [System.Drawing.ColorTranslator]::FromHtml('#b86b24')
$graphics.Clear($cream)

$panelBrush = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    [System.Drawing.Rectangle]::new(24, 24, 976, 452), $green, $greenDeep, 12
)
$panelPath = [System.Drawing.Drawing2D.GraphicsPath]::new()
$panelPath.AddArc(24, 24, 38, 38, 180, 90)
$panelPath.AddArc(962, 24, 38, 38, 270, 90)
$panelPath.AddArc(962, 438, 38, 38, 0, 90)
$panelPath.AddArc(24, 438, 38, 38, 90, 90)
$panelPath.CloseFigure()
$graphics.FillPath($panelBrush, $panelPath)

$circlePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(26, 244, 234, 216), 2)
$graphics.DrawEllipse($circlePen, 756, 48, 340, 340)
$graphics.DrawEllipse($circlePen, 786, 78, 280, 280)

$logoPen = [System.Drawing.Pen]::new($cream, 5)
$graphics.DrawEllipse($logoPen, 66, 66, 74, 74)
$needle = [System.Drawing.Point[]]@(
    [System.Drawing.Point]::new(117, 83),
    [System.Drawing.Point]::new(101, 114),
    [System.Drawing.Point]::new(83, 124),
    [System.Drawing.Point]::new(99, 92)
)
$graphics.FillPolygon([System.Drawing.SolidBrush]::new($cream), $needle)
$graphics.FillPolygon([System.Drawing.SolidBrush]::new($saffron), [System.Drawing.Point[]]@(
    [System.Drawing.Point]::new(101, 114),
    [System.Drawing.Point]::new(99, 92),
    [System.Drawing.Point]::new(83, 124)
))

$displayFont = [System.Drawing.Font]::new('Georgia', 35, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$bodyFont = [System.Drawing.Font]::new('Segoe UI', 17, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$smallFont = [System.Drawing.Font]::new('Segoe UI', 12, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$labelFont = [System.Drawing.Font]::new('Segoe UI', 16, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$creamBrush = [System.Drawing.SolidBrush]::new($cream)
$softBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(205, 244, 240, 230))
$graphics.DrawString('CAP COMPASS', $smallFont, $softBrush, 164, 73)
$graphics.DrawString("Plan your CAP list`nwith a clear direction", $displayFont, $creamBrush, 158, 111)
$graphics.DrawString("Explore sample cut-offs, arrange choices,`nand track each step.", $bodyFont, $softBrush, 164, 225)
$graphics.DrawString('MHT-CET  |  ENGINEERING ADMISSIONS', $smallFont, $softBrush, 164, 278)

$cardShadow = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(48, 0, 0, 0))
Fill-RoundedRectangle $graphics $cardShadow ([System.Drawing.Rectangle]::new(642, 61, 306, 380)) 22
$card = [System.Drawing.SolidBrush]::new($surface)
Fill-RoundedRectangle $graphics $card ([System.Drawing.Rectangle]::new(632, 52, 306, 380)) 22
$cardLabelFont = [System.Drawing.Font]::new('Segoe UI', 11, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$graphics.DrawString('YOUR PREFERENCE ORDER', $cardLabelFont, [System.Drawing.SolidBrush]::new($muted), 654, 74)

$rowLabels = @(
    @{ Rank = '01'; Title = 'Dream choice'; Subtitle = 'Aim high, then keep going'; Color = '#b86b24' },
    @{ Rank = '02'; Title = 'Realistic choice'; Subtitle = 'A course you would accept'; Color = '#2b64a2' },
    @{ Rank = '03'; Title = 'Strong option'; Subtitle = 'Keep another door open'; Color = '#166044' }
)
$rowY = 111
foreach ($row in $rowLabels) {
    $rowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 250, 246, 237))
    Fill-RoundedRectangle $graphics $rowBrush ([System.Drawing.Rectangle]::new(650, $rowY, 270, 78)) 13
    $stripe = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($row.Color))
    Fill-RoundedRectangle $graphics $stripe ([System.Drawing.Rectangle]::new(650, $rowY, 5, 78)) 2
    $graphics.DrawString($row.Rank, $smallFont, [System.Drawing.SolidBrush]::new($muted), 668, $rowY + 16)
    $graphics.DrawString($row.Title, $labelFont, [System.Drawing.SolidBrush]::new($ink), 706, $rowY + 11)
    $graphics.DrawString($row.Subtitle, $smallFont, [System.Drawing.SolidBrush]::new($muted), 706, $rowY + 39)
    $rowBrush.Dispose()
    $stripe.Dispose()
    $rowY += 88
}
$graphics.DrawString('Your order. Your plan.', $bodyFont, [System.Drawing.SolidBrush]::new($green), 670, 390)

$bitmap.Save((Join-Path $assetDir 'feature-graphic.png'), [System.Drawing.Imaging.ImageFormat]::Png)

$graphics.Dispose()
$bitmap.Dispose()
$panelBrush.Dispose()
$panelPath.Dispose()
$circlePen.Dispose()
$logoPen.Dispose()
$displayFont.Dispose()
$bodyFont.Dispose()
$smallFont.Dispose()
$labelFont.Dispose()
$cardLabelFont.Dispose()
$creamBrush.Dispose()
$softBrush.Dispose()
$cardShadow.Dispose()
$card.Dispose()

Write-Output (Join-Path $assetDir 'icon-512.png')
Write-Output (Join-Path $assetDir 'feature-graphic.png')
