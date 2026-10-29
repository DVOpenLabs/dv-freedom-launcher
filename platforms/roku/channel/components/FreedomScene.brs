sub init()
    m.top.backgroundColor = "__COLOR__"
    m.top.backgroundUri = ""
    m.clock24 = __CLOCK24__
    m.colors = ["0x101C2CFF", "0x123A51FF", "0x173B31FF"]
    m.index = 0
    m.minute = m.top.FindNode("minute")
    m.minute.ObserveField("fire", "updateClock")
    m.minute.control = "start"
    updateClock()
    m.top.SetFocus(true)
end sub

sub updateClock()
    dt = CreateObject("roDateTime")
    dt.ToLocalTime()
    hour = dt.GetHours()
    suffix = ""
    if not m.clock24
        suffix = " AM"
        if hour >= 12 then suffix = " PM"
        hour = hour mod 12
        if hour = 0 then hour = 12
    end if
    minute = Right("0" + dt.GetMinutes().ToStr(), 2)
    m.top.FindNode("clock").text = hour.ToStr() + ":" + minute + suffix
    m.top.FindNode("date").text = dt.AsDateString("long-date")
    ' Shift a small amount each minute; never override the TV's screen protection.
    group = m.top.FindNode("clockGroup")
    group.translation = [180 + (dt.GetMinutes() mod 6) * 8, 220 + (dt.GetMinutes() mod 4) * 6]
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if press and key = "OK"
        m.index = (m.index + 1) mod m.colors.Count()
        m.top.backgroundColor = m.colors[m.index]
        return true
    end if
    return false
end function
